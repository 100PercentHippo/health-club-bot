package com.c2t2s.hb;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.AutocompleteInteraction;
import org.javacord.api.interaction.InteractionBase;
import org.javacord.api.interaction.MessageComponentInteraction;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionChoice;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;

import java.awt.Color;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.Set;

public class HBMain {

    static final Random RNG_SOURCE = new Random();

    static int generateBoundedNormal(int average, int stdDev, int min) {
        int roll = (int)(HBMain.RNG_SOURCE.nextGaussian() * stdDev) + average;
        if (roll < min) {
            return min;
        }
        return roll;
    }

    static class SingleResponse {
        String message;
        ActionRow buttons;

        SingleResponse() {
            message = "";
            buttons = null;
        }

        SingleResponse(String message) {
            this.message = message;
            buttons = null;
        }

        SingleResponse(String message, ActionRow buttons) {
            this.message = message;
            this.buttons = buttons;
        }
    }

    static class MultistepResponse {
        List<String> messages;
        ActionRow buttons = null;
        long delay = 1000; // milliseconds
        int index = -1; // Initially before first message
        Map<Integer, URL> images = new HashMap<>();

        MultistepResponse() {
            messages = new ArrayList<>();
        }

        MultistepResponse(String message) {
            messages = new ArrayList<>();
            messages.add(message);
        }

        MultistepResponse(String message, ActionRow buttons) {
            messages = new ArrayList<>();
            messages.add(message);
            this.buttons = buttons;
        }

        MultistepResponse(List<String> messages) {
            this.messages = messages;
            this.buttons = null;
        }

        MultistepResponse(List<String> messages, ActionRow buttons) {
            this.messages = messages;
            this.buttons = buttons;
        }

        void addMessage(String message) {
            messages.add(message);
        }

        void addMessageToStart(String message) {
            messages.add(0, message);
        }

        void addAllToStart(List<String> messages) {
            this.messages.addAll(0, messages);
        }

        boolean isAtEnd() {
            return index + 1 >= messages.size();
        }

        boolean next() {
            return ++index < messages.size();
        }

        boolean hasMessages() {
            return messages != null;
        }

        String getMessage() {
            return messages.get(index);
        }

        boolean hasAttachment() {
            return images.containsKey(index);
        }

        URL getAttachment() {
            return images.get(index);
        }
    }

    static class EmbedResponse {
        private String title;
        private String message;
        private Color color;
        private ActionRow buttons;

        EmbedResponse(Color color, String message) {
            this.message = message;
            this.color = color;
        }

        EmbedResponse(Color color, String message, String title) {
            this.title = title;
            this.message = message;
            this.color = color;
        }

        Color getColor() {
            return color;
        }

        String getTitle() {
            return title;
        }

        String getMessage() {
            return message;
        }

        ActionRow getButtons() {
            return buttons;
        }

        boolean hasTitle() {
            return title != null && !title.isEmpty();
        }

        boolean hasButtons() {
            return buttons != null;
        }
    }

    static class AutocompleteIdOption {
        long id;
        String description;

        AutocompleteIdOption(long id, String description) {
            this.id = id;
            this.description = description;
        }

        long getId() {
            return id;
        }

        String getDescription() {
            return description;
        }
    }

    abstract static class CasinoCommand {
        Consumer<SlashCommandInteraction> responder;
        // If the number of channel options expands greatly, replace these with a bitmask
        boolean isValidInCasinoChannels = true;
        boolean isValidInGachaChannels = false;

        boolean isValidInCasinoChannels() {
            return isValidInCasinoChannels;
        }

        boolean isValidInGachaChannels() {
            return isValidInGachaChannels;
        }

        void handle(SlashCommandInteraction interaction) {
            if (responder != null) {
                responder.accept(interaction);
            }
        }
    }

    private static class SimpleCasinoCommand extends CasinoCommand {
        SimpleCasinoCommand(Supplier<String> handler) {
            this.responder = i -> respondImmediately(new SingleResponse(handler.get()), i);
        }

        SimpleCasinoCommand(Supplier<String> handler, boolean ephemeral) {
            this.responder = i -> respondImmediately(new SingleResponse(handler.get()), i, ephemeral);
        }

        SimpleCasinoCommand(Supplier<String> handler, boolean ephemeral, boolean validInCasino, boolean validInGacha) {
            this.responder = i -> respondImmediately(new SingleResponse(handler.get()), i, ephemeral);
            this.isValidInCasinoChannels = validInCasino;
            this.isValidInGachaChannels = validInGacha;
        }

        SimpleCasinoCommand(Function<SlashCommandInteraction, String> handler) {
            this.responder = i -> respondImmediately(new SingleResponse(handler.apply(i)), i);
        }

        SimpleCasinoCommand(Function<SlashCommandInteraction, String> handler, boolean ephemeral) {
            this.responder = i -> respondImmediately(new SingleResponse(handler.apply(i)), i, ephemeral);
        }

        SimpleCasinoCommand(Function<SlashCommandInteraction, String> handler, boolean ephemeral, boolean validInCasino, boolean validInGacha) {
            this.responder = i -> respondImmediately(new SingleResponse(handler.apply(i)), i, ephemeral);
            this.isValidInCasinoChannels = validInCasino;
            this.isValidInGachaChannels = validInGacha;
        }
    }

    private static class ImmediateCasinoCommand extends CasinoCommand {
        ImmediateCasinoCommand(Function<SlashCommandInteraction, SingleResponse> handler) {
            this.responder = i -> respondImmediately(handler.apply(i), i);
        }

        ImmediateCasinoCommand(Function<SlashCommandInteraction, SingleResponse> handler, boolean ephemeral) {
            this.responder = i -> respondImmediately(handler.apply(i), i, ephemeral);
        }

        ImmediateCasinoCommand(Function<SlashCommandInteraction, SingleResponse> handler, boolean ephemeral, boolean validInCasino, boolean validInGacha) {
            this.responder = i -> respondImmediately(handler.apply(i), i, ephemeral);
            this.isValidInCasinoChannels = validInCasino;
            this.isValidInGachaChannels = validInGacha;
        }
    }

    private static class MultistepCasinoCommand extends CasinoCommand {
        MultistepCasinoCommand(Function<SlashCommandInteraction, MultistepResponse> handler) {
            this.responder = i -> i.respondLater().thenAccept(updater -> makeMultiStepResponse(handler.apply(i), updater));
        }

        MultistepCasinoCommand(Function<SlashCommandInteraction, MultistepResponse> handler, boolean validInCasino, boolean validInGacha) {
            this.responder = i -> i.respondLater().thenAccept(updater -> makeMultiStepResponse(handler.apply(i), updater));
            this.isValidInCasinoChannels = validInCasino;
            this.isValidInGachaChannels = validInGacha;
        }
    }

    private static final String VERSION_COMMAND = "version";
    private static final String HELP_COMMAND = "help";
    private static final String CHANGELOG_COMMAND = "changelog";
    private static final String LATEST_RELEASE_COMMAND = "latestrelease";
    private static final String ROLL_COMMAND = "roll";
    private static final String CLAIM_COMMAND = "claim";
    private static final String BALANCE_COMMAND = "balance";
    private static final String LEADERBOARD_COMMAND = "leaderboard";
    private static final String GIVE_COMMAND = "give";
    private static final String POT_COMMAND = "pot";
    private static final String FEED_COMMAND = "feed";
    private static final String WORK_COMMAND = "work";
    private static final String FISH_COMMAND = "fish";
    private static final String PICKPOCKET_COMMAND = "pickpocket";
    private static final String ROB_COMMAND = "rob";
    private static final String GUESS_COMMAND = "guess";
    private static final String HUGEGUESS_COMMAND = "hugeguess";
    private static final String SLOTS_COMMAND = "slots";
    private static final String MINISLOTS_COMMAND = "minislots";
    private static final String OVERUNDER_COMMAND = "overunder";
    private static final String BLACKJACK_COMMAND = "blackjack";
    private static final String ALLORNOTHING_COMMAND = "allornothing";
    private static final String STATS_COMMAND = "stats";
    private static final String WORKOUT_COMMAND = "workout";
    private static final String SELECT_WORKOUT_REWARD_COMMAND = "selectworkoutreward";
    private static final String PULL_COMMAND = "pull";
    private static final String PULLS_COMMAND = "pulls";
    private static final String PITY_COMMAND = "pity";
    private static final String GACHA_CHARACTER_LIST_COMMAND = "gacha character list";
    private static final String GACHA_CHARACTER_INFO_COMMAND = "gacha character info";
    private static final String GACHA_BANNER_LIST_COMMAND = "gacha banner list";
    private static final String GACHA_BANNER_INFO_COMMAND = "gacha banner info";
    private static final String REGISTER_CHANNEL_COMMAND = "registerchannel";
    private static final String TEST_COMMAND = "test";

    private static final int REGISTER_SUBCOMMAND_ADD_CASINO_CHANNEL = 0;
    private static final int REGISTER_SUBCOMMAND_ADD_EVENT_CHANNEL = 1;
    private static final int REGISTER_SUBCOMMAND_REMOVE_CASINO_CHANNEL = 2;
    private static final int REGISTER_SUBCOMMAND_REMOVE_EVENT_CHANNEL = 3;

    private static final long DEFAULT_LEADERBOARD_LENGTH = 3L;
    private static final long DEFAULT_CASINO_WAGER = 100L;
    private static final long DEFAULT_ALLORNOTHING_WAGER = 500L;
    private static final long DEFAULT_PULL_AMOUNT = 1L;

    static DiscordApi api;

    private static Map<String, CasinoCommand> commands = Map.ofEntries(
            entry(VERSION_COMMAND, new SimpleCasinoCommand(
                Changelog::getVersion)),
            entry(HELP_COMMAND, new SimpleCasinoCommand(
                HBMain::getHelpText,
                true)),
            entry(CHANGELOG_COMMAND, new SimpleCasinoCommand(
                i -> Changelog.getChangelog(i.getArgumentStringValueByIndex(0).orElse("")),
                true)),
            entry(LATEST_RELEASE_COMMAND, new SimpleCasinoCommand(
                Changelog::getLatestRelease)),
            entry(ROLL_COMMAND, new ImmediateCasinoCommand(
                i -> Roll.handleRoll(i.getArgumentStringValueByIndex(0).get()))),
            entry(CLAIM_COMMAND, new SimpleCasinoCommand(
                i -> Casino.handleClaim(i.getUser().getId(), i.getUser().getDiscriminatedName()))),
            entry(BALANCE_COMMAND, new SimpleCasinoCommand(
                i -> Casino.handleBalance(i.getUser().getId()))),
            entry(LEADERBOARD_COMMAND, new SimpleCasinoCommand(
                i -> Casino.handleLeaderboard(i.getArgumentLongValueByIndex(0).orElse(DEFAULT_LEADERBOARD_LENGTH)))),
            entry(GIVE_COMMAND, new SimpleCasinoCommand(
                i -> Casino.handleGive(i.getUser().getId(), i.getArgumentUserValueByIndex(0).get().getId(),
                                       i.getArgumentLongValueByIndex(1).get()))),
            entry(POT_COMMAND, new SimpleCasinoCommand(
                Casino::handlePot)),
            entry(FEED_COMMAND, new SimpleCasinoCommand(
                i -> Casino.handleFeed(i.getUser().getId(), i.getArgumentLongValueByIndex(0).get()))),
            entry(WORK_COMMAND, new SimpleCasinoCommand(
                i -> Casino.handleWork(i.getUser().getId()))),
            entry(FISH_COMMAND, new SimpleCasinoCommand(
                i -> Casino.handleFish(i.getUser().getId()))),
            entry(PICKPOCKET_COMMAND, new SimpleCasinoCommand(
                i -> Casino.handlePickpocket(i.getUser().getId()))),
            entry(ROB_COMMAND, new SimpleCasinoCommand(
                i -> Casino.handleRob(i.getUser().getId()))),
            entry(GUESS_COMMAND, new SimpleCasinoCommand(
                i -> Casino.handleGuess(i.getUser().getId(), i.getArgumentLongValueByIndex(0).get(),
                                        i.getArgumentLongValueByIndex(1).orElse(DEFAULT_CASINO_WAGER)))),
            entry(HUGEGUESS_COMMAND, new SimpleCasinoCommand(
                i -> Casino.handleHugeGuess(i.getUser().getId(), i.getArgumentLongValueByIndex(0).get(),
                                            i.getArgumentLongValueByIndex(1).orElse(DEFAULT_CASINO_WAGER)))),
            entry(SLOTS_COMMAND, new MultistepCasinoCommand(
                i -> Casino.handleSlots(i.getUser().getId(), i.getArgumentLongValueByIndex(0).orElse(DEFAULT_CASINO_WAGER)))),
            entry(MINISLOTS_COMMAND, new MultistepCasinoCommand(
                i -> Casino.handleMinislots(i.getUser().getId(), i.getArgumentLongValueByIndex(0).orElse(DEFAULT_CASINO_WAGER)))),
            entry(OVERUNDER_COMMAND, new ImmediateCasinoCommand(
                i -> Casino.handleOverUnderInitial(i.getUser().getId(), i.getArgumentLongValueByIndex(0).orElse(DEFAULT_CASINO_WAGER)))),
            entry(BLACKJACK_COMMAND, new ImmediateCasinoCommand(
                i -> Blackjack.handleBlackjack(i.getUser().getId(), i.getArgumentLongValueByIndex(0).orElse(DEFAULT_CASINO_WAGER)))),
            entry(ALLORNOTHING_COMMAND, new MultistepCasinoCommand(
                i -> AllOrNothing.handleNew(i.getUser().getId(), i.getArgumentLongValueByIndex(0).get(),
                                            i.getArgumentLongValueByIndex(1).orElse(DEFAULT_ALLORNOTHING_WAGER)))),
            entry(STATS_COMMAND, new SimpleCasinoCommand(
                i -> Stats.handleStats(i.getArgumentStringValueByIndex(0).orElse(""), i.getUser().getId()))),
            entry(WORKOUT_COMMAND, new ImmediateCasinoCommand(
                i -> HealthClub.handleWorkout(i.getUser().getId()),
                true)),
            entry(SELECT_WORKOUT_REWARD_COMMAND, new SimpleCasinoCommand(
                i -> HealthClub.handleSelectReward(i.getUser().getId(), i.getArgumentLongValueByIndex(0).get()),
                true)),
            entry(PULL_COMMAND, new MultistepCasinoCommand(
                i -> Gacha.handleGachaPull(i.getUser().getId(), i.getArgumentLongValueByIndex(0).get(),
                                           i.getArgumentLongValueByIndex(1).orElse(DEFAULT_PULL_AMOUNT)))),
            entry(PULLS_COMMAND, new SimpleCasinoCommand(
                i -> Gacha.handlePulls(i.getUser().getId()))),
            entry(PITY_COMMAND, new SimpleCasinoCommand(
                i -> Gacha.handlePity(i.getUser().getId(), i.getArgumentLongValueByIndex(0).get()))),
            entry(GACHA_CHARACTER_INFO_COMMAND, new MultistepCasinoCommand(
                i -> Gacha.handleCharacterInfo(i.getUser().getId(), i.getArgumentLongValueByIndex(0).get()))),
            entry(GACHA_CHARACTER_LIST_COMMAND, new SimpleCasinoCommand(
                i -> Gacha.handleCharacterList(i.getUser().getId()))),
            entry(GACHA_BANNER_INFO_COMMAND, new SimpleCasinoCommand(
                i -> Gacha.handleBannerInfo(i.getArgumentLongValueByIndex(0).get()))),
            entry(GACHA_BANNER_LIST_COMMAND, new SimpleCasinoCommand(
                i -> Gacha.handleBannerList(i.getUser().getId()))),
            entry(REGISTER_CHANNEL_COMMAND, new SimpleCasinoCommand(
                i -> handleRegisterChannel(i.getUser().getId(), i.getServer(), i.getChannel(), i.getArgumentLongValueByIndex(0).get()),
                false,
                false,
                false)),
            entry(TEST_COMMAND, new SimpleCasinoCommand(
                GachaItems::handleTest,
                false,
                false,
                false))
        );

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("API key is required as first argument");
            return;
        }

        CommandAccessControl.initialize();

        api = new DiscordApiBuilder().setToken(args[0]).login().join();
        api.setMessageCacheSize(0, 0);
        if (args.length > 1 && args[1].equalsIgnoreCase("init")) {
            initCommands(api);
        }

        api.addSlashCommandCreateListener(event -> {
            SlashCommandInteraction interaction = event.getSlashCommandInteraction();
            CasinoCommand command = commands.get(interaction.getFullCommandName());

            if (command == null) {
                respondImmediately(new SingleResponse(
                    "Command `" + interaction.getFullCommandName() + "` not found"), interaction);
                return;
            }

            if (!interaction.getServer().isPresent()) {
                respondImmediately(
                    new SingleResponse("Unable to run `" + interaction.getFullCommandName()
                        + "` in this channel. Server was not provided through API"),
                    interaction, true);
            } else if (!interaction.getChannel().isPresent()) {
                respondImmediately(
                    new SingleResponse("Unable to run `" + interaction.getFullCommandName()
                        + "` in this channel. Channel was not provided through API"),
                    interaction, true);
            } else if (!CommandAccessControl.isValid(interaction.getUser().getId(), command,
                    interaction.getServer().get().getId(), interaction.getChannel().get().getId())) {
                respondImmediately(
                    new SingleResponse("Unable to run `" + interaction.getFullCommandName()
                        + "` in this channel. If this is unexpected, have a casino admin register this channel"),
                    interaction, true);
                return;
            }

            command.handle(interaction);
        });
        api.addMessageComponentCreateListener(event -> {
            MessageComponentInteraction interaction = event.getMessageComponentInteraction();
            String prefix = interaction.getCustomId();
            int seperator = prefix.indexOf('.');
            if (seperator > 0) {
                prefix = prefix.substring(0, seperator);
            }
            switch (prefix) {
                case OVERUNDER_COMMAND:
                    handleOverUnderButtonPress(interaction);
                    break;
                case BLACKJACK_COMMAND:
                    handleBlackjackButtonPress(interaction);
                    break;
                case ALLORNOTHING_COMMAND:
                    handleAllOrNothingButtonPress(interaction);
                    break;
                case WORKOUT_COMMAND:
                    handleWorkoutButtonPress(interaction);
                    break;
                case ROLL_COMMAND:
                    handleRollButtonPress(interaction);
                    break;
                default:
                    System.out.println("Encountered unexpected interaction prefix: " + prefix + "\nFull id: " + interaction.getCustomId());
            }
        });
        api.addAutocompleteCreateListener(event -> {
            AutocompleteInteraction interaction = event.getAutocompleteInteraction();
            List<SlashCommandOptionChoice> choices = new ArrayList<>();
            switch (interaction.getFullCommandName()) {
                case STATS_COMMAND:
                    Arrays.stream(Stats.StatsOption.values())
                        .forEach(o -> choices.add(SlashCommandOptionChoice.create(o.getDescription(), o.getName())));
                    break;
                case PULL_COMMAND:
                case PITY_COMMAND:
                case GACHA_BANNER_INFO_COMMAND:
                    Gacha.getBanners().forEach(o -> choices.add(SlashCommandOptionChoice.create(o.getDescription(), o.getId())));
                    break;
                case GACHA_CHARACTER_INFO_COMMAND:
                    Gacha.getCharacters(interaction.getUser().getId())
                        .forEach(o -> choices.add(SlashCommandOptionChoice.create(o.getDescription(), o.getId())));
                    break;
                default:
                    return;
            }
            interaction.respondWithChoices(choices);
        });
        System.out.println("Server started");
    }

    private static void handleOverUnderButtonPress(MessageComponentInteraction interaction) {
        int prediction = 0;
        switch (interaction.getCustomId()) {
            case "overunder.over":
                prediction = Casino.PREDICTION_OVER;
                break;
            case "overunder.under":
                prediction = Casino.PREDICTION_UNDER;
                break;
            case "overunder.same":
                prediction = Casino.PREDICTION_SAME;
                break;
            default:
                System.out.println("Encountered unexpected overunder interaction: "
                    + interaction.getCustomId());
        }
        respondImmediately(Casino.handleOverUnderFollowup(interaction.getUser().getId(), prediction), interaction);
    }

    private static void handleBlackjackButtonPress(MessageComponentInteraction interaction) {
        switch (interaction.getCustomId()) {
            case "blackjack.hit":
                interaction.respondLater().thenAccept(updater ->
                    makeMultiStepResponse(Blackjack.handleHit(interaction.getUser().getId()),
                    updater)
                );
                break;
            case "blackjack.stand":
                interaction.respondLater().thenAccept(updater ->
                    makeMultiStepResponse(Blackjack.handleStand(interaction.getUser().getId()),
                    updater)
                );
                break;
            case "blackjack.split":
                respondImmediately(Blackjack.handleSplit(interaction.getUser().getId()), interaction);
                break;
            default:
                System.out.println("Encountered unexpected blackjack interaction: "
                    + interaction.getCustomId());
        }
    }

    private static void handleAllOrNothingButtonPress(MessageComponentInteraction interaction) {
        String[] parts = interaction.getCustomId().split("\\|");
        if (parts.length < 1) {
            System.out.println(String.format("Encountered unexpected allornothing interaction: %s (split into %s)",
                interaction.getCustomId(), Arrays.toString(parts)));
            return;
        }
        String command = parts[0];
        int rollsToDouble;
        try {
            if (parts.length > 1) {
                rollsToDouble = Integer.parseInt(parts[1]);
            } else {
                rollsToDouble = 0;
            }
        } catch (NumberFormatException e) {
            System.out.println("Unable to parse allornothing odds as int: " + parts[1]
                + " (full command " + interaction.getCustomId() + ")");
            return;
        }
        switch (command) {
            case "allornothing.claim":
                respondImmediately(new SingleResponse(AllOrNothing.handleCashOut(interaction.getUser().getId(), rollsToDouble)),
                    interaction);
                break;
            case "allornothing.roll":
                interaction.respondLater().thenAccept(updater ->
                    makeMultiStepResponse(AllOrNothing.handleRoll(interaction.getUser().getId(), rollsToDouble),
                    updater)
                );
                break;
            default:
                System.out.println(String.format("Encountered unexpected allornothing command: %s (full command %s)",
                    command, interaction.getCustomId()));
                return;
        }
    }

    private static void handleWorkoutButtonPress(MessageComponentInteraction interaction) {
        switch (interaction.getCustomId()) {
            case "workout.restore":
                respondImmediately(HealthClub.handleRestoreStreak(interaction.getUser().getId()), interaction, true);
                break;
            case "workout.break":
                respondImmediately(HealthClub.handleBreakStreak(interaction.getUser().getId()), interaction, true);
                break;
            default:
                System.out.println("Encountered unexpected workout interaction: "
                    + interaction.getCustomId());
        }
    }

    private static void handleRollButtonPress(MessageComponentInteraction interaction) {
        String[] parts = interaction.getCustomId().split("\\|");
        if (parts.length < 2) {
            System.out.println(String.format("Encountered unexpected deathroll interaction: %s (split into %s)",
                interaction.getCustomId(), Arrays.toString(parts)));
            return;
        }
        String command = parts[0];
        int maxRoll;
        try {
            maxRoll = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            System.out.println("Unable to parse deathroll max as int: " + parts[1]
                + " (full command " + interaction.getCustomId() + ")");
            return;
        }
        if (command.equals("roll.deathroll")) {
            respondImmediately(Roll.handleDeathroll(maxRoll), interaction);
        } else {
            System.out.println(String.format("Encountered unexpected roll command: %s (full command %s)",
                command, interaction.getCustomId()));
        }
    }

    private static String handleRegisterChannel(long uid, Optional<Server> serverOptional,
            Optional<TextChannel> channelOptional, long subcommand) {
        if (serverOptional.isEmpty() || channelOptional.isEmpty()) {
            return "Unable to register channel: Server or channel is empty";
        }
        if (!(channelOptional.get() instanceof ServerTextChannel)) {
            return "Unable to register channel: Channel is not a text channel";
        }
        Server server = serverOptional.get();
        ServerTextChannel channel = (ServerTextChannel)channelOptional.get();
        switch ((int)subcommand) {
            case REGISTER_SUBCOMMAND_ADD_CASINO_CHANNEL:
                return CommandAccessControl.handleRegisterCasinoChannel(uid, server.getId(),
                    server.getName(), channel.getId(), channel.getName());
            case REGISTER_SUBCOMMAND_ADD_EVENT_CHANNEL:
                return CommandAccessControl.handleRegisterEventChannel(uid, server.getId(),
                    server.getName(), channel.getId(), channel.getName());
            case REGISTER_SUBCOMMAND_REMOVE_CASINO_CHANNEL:
                return CommandAccessControl.handleDeregisterCasinoChannel(uid, server.getId(),
                    channel.getId());
            case REGISTER_SUBCOMMAND_REMOVE_EVENT_CHANNEL:
                return CommandAccessControl.handleDeregisterEventChannel(uid, server.getId(),
                    channel.getId());
            default:
                throw new IllegalArgumentException("Unexpected registercommand subcommand received: "
                    + subcommand);
        }
    }

    private static void initCommands(DiscordApi api) {
        System.out.println("Registering commands with discord");
        Set<SlashCommandBuilder> builders = new HashSet<>();

        builders.add(new SlashCommandBuilder().setName(VERSION_COMMAND)
            .setDescription("Check the current bot version").setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(HELP_COMMAND)
            .setDescription("Print available Casino Bot commands").setEnabledInDms(true));
        builders.add(new SlashCommandBuilder().setName(CHANGELOG_COMMAND)
            .setDescription("Print recent Casino Bot changelog").setEnabledInDms(true)
            .addOption(SlashCommandOption.createWithChoices(SlashCommandOptionType.STRING, "Versions", "Changelog version range", false,
                Arrays.asList(SlashCommandOptionChoice.create("3.2.0-latest", "3.2.0-latest"),
                    SlashCommandOptionChoice.create("3.1.8-3.1.11", "3.1.8-3.1.11"),
                    SlashCommandOptionChoice.create("3.1.0-3.1.7", "3.1.0-3.1.7"),
                    SlashCommandOptionChoice.create("2.0.0-2.0.13", "2.0.0-2.0.13"),
                    SlashCommandOptionChoice.create("1.5.0-1.5.5", "1.5.0-1.5.5"),
                    SlashCommandOptionChoice.create("1.4.0-1.4.8", "1.4.0-1.4.8"),
                    SlashCommandOptionChoice.create("1.0.0-1.3.2", "1.0.0-1.3.2")))));
        builders.add(new SlashCommandBuilder().setName(LATEST_RELEASE_COMMAND)
            .setDescription("Print the changelog of the most recent Casino Bot update").setEnabledInDms(true));
        builders.add(new SlashCommandBuilder().setName(ROLL_COMMAND)
            .setDescription("Roll a random number. Supports deathrolling (`/roll 10`) or RPG style dice (`/roll 1d20`)")
            .addOption(SlashCommandOption.createStringOption("argument", "What to roll. Either a number (`100`) or an RPG style sequence (`1d20`)", true))
            .setEnabledInDms(true));
        builders.add(new SlashCommandBuilder().setName(GUESS_COMMAND).setDescription("Guess a number from 1 to 10!")
            .addOption(SlashCommandOption.createLongOption("guess", "What you think the number will be", true, 1, 10))
            .addOption(SlashCommandOption.createLongOption("wager", "Amount to wager, default 100", false, 1, 100000))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(HUGEGUESS_COMMAND).setDescription("Guess a number from 1 to 100!")
            .addOption(SlashCommandOption.createLongOption("guess", "What you think the number will be", true, 1, 100))
            .addOption(SlashCommandOption.createLongOption("wager", "Amount to wager, default 100", false, 1, 100000))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(SLOTS_COMMAND).setDescription("Spin the slots!")
            .addOption(SlashCommandOption.createLongOption("wager", "Amount to wager, default 100", false, 1, 100000))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(MINISLOTS_COMMAND).setDescription("Spin the little slots!")
            .addOption(SlashCommandOption.createLongOption("wager", "Amount to wager, default 100", false, 1, 100000))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(CLAIM_COMMAND).setDescription("Initialize yourself as a casino user")
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(BALANCE_COMMAND).setDescription("Check your current balance")
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(WORK_COMMAND).setDescription("Work for 2 hours to earn some coins")
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(FISH_COMMAND).setDescription("Fish for 30 minutes to earn some coins")
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(ROB_COMMAND).setDescription("Attempt to rob The Bank to steal some of The Money. You might be caught!")
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(PICKPOCKET_COMMAND).setDescription("Attempt a petty theft of pickpocketting")
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(LEADERBOARD_COMMAND).setDescription("View the richest people in the casino")
            .addOption(SlashCommandOption.createLongOption("entries", "Number of entries to show, default 3", false, 1, 10))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(POT_COMMAND).setDescription("Check how much money is in the Money Machine")
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(FEED_COMMAND).setDescription("Feed the Money Machine")
            .addOption(SlashCommandOption.createLongOption("amount", "How much to feed", true, 1, 100000))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(OVERUNDER_COMMAND).setDescription("Multiple rounds of predicting if the next number is over or under")
            .addOption(SlashCommandOption.createLongOption("wager", "Amount to wager, default 100", false, 1, 100000))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(BLACKJACK_COMMAND).setDescription("Play a game of blackjack")
            .addOption(SlashCommandOption.createLongOption("wager", "Amount to wager, default 100", false, 1, 100000))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(GIVE_COMMAND).setDescription("Give coins to another user")
            .addOption(SlashCommandOption.createUserOption("recipient", "Person to give coins to", true))
            .addOption(SlashCommandOption.createLongOption("amount", "Amount to transfer", true, 1, 100000))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(PULL_COMMAND).setDescription("Try to win a gacha character!")
            .addOption(SlashCommandOption.createLongOption("banner", "Which banner to pull on", true, true))
            .addOption(SlashCommandOption.createLongOption("pulls", "Number of pulls to use, default 1, max 25", false, 1, 25))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(PULLS_COMMAND).setDescription("Check how many gacha pulls you have")
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(PITY_COMMAND).setDescription("Check your gacha pity")
            .addOption(SlashCommandOption.createLongOption("banner", "Which banner to view", true, true))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("gacha").setDescription("Character management commands")
            .addOption(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND_GROUP, "character", "Interact with your characters",
                Arrays.asList(SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "list", "List your characters"),
                    SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "info", "View details of a single character",
                        Arrays.asList(SlashCommandOption.createLongOption("character", "Which character to view", true, true))))))
            .addOption(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND_GROUP, "banner", "View the available banners",
                Arrays.asList(SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "list", "List available banners"),
                    SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "info", "View details of a single banner",
                        Arrays.asList(SlashCommandOption.createLongOption("banner", "Which banner to view", true, true))))))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(ALLORNOTHING_COMMAND).setDescription("Test your luck, and maybe set a high score")
            .addOption(SlashCommandOption.createWithChoices(SlashCommandOptionType.LONG, "odds", "Chance to win each roll", true,
                Arrays.asList(SlashCommandOptionChoice.create("70%", 2), SlashCommandOptionChoice.create("80%", 3),
                    SlashCommandOptionChoice.create("90%", 7))))
            .addOption(SlashCommandOption.createLongOption("wager", "Amount to wager, default 500", false, 500, 100000))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(STATS_COMMAND).setDescription("Check the odds of a given game")
            .addOption(SlashCommandOption.createStringOption("Game", "Which game to display associated stats", true, true))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName(WORKOUT_COMMAND).setDescription("Record a workout (or other self-improvement activity), and receive a reward")
            .setEnabledInDms(true));
        builders.add(new SlashCommandBuilder().setName(SELECT_WORKOUT_REWARD_COMMAND).setDescription("Select what reward to receive when reporting workouts")
            .addOption(SlashCommandOption.createWithChoices(SlashCommandOptionType.LONG, "reward", "Desired reward", true,
                Arrays.asList(SlashCommandOptionChoice.create(HealthClub.getRewardDescription(HealthClub.COIN_REWARD_ID), HealthClub.COIN_REWARD_ID),
                    SlashCommandOptionChoice.create(HealthClub.getRewardDescription(HealthClub.PULL_REWARD_ID), HealthClub.PULL_REWARD_ID)))));
        builders.add(new SlashCommandBuilder().setName(REGISTER_CHANNEL_COMMAND).setDescription("Register a channel for use by the casino bot")
            .addOption(SlashCommandOption.createWithChoices(SlashCommandOptionType.LONG, "subcommand", "Action to perform", true,
                Arrays.asList(SlashCommandOptionChoice.create("Register this as a casino channel", REGISTER_SUBCOMMAND_ADD_CASINO_CHANNEL),
                    SlashCommandOptionChoice.create("Register this as an event channel", REGISTER_SUBCOMMAND_ADD_EVENT_CHANNEL),
                    SlashCommandOptionChoice.create("Deregister this casino channel", REGISTER_SUBCOMMAND_REMOVE_CASINO_CHANNEL),
                    SlashCommandOptionChoice.create("Deregister this event channel", REGISTER_SUBCOMMAND_REMOVE_EVENT_CHANNEL)))));
        builders.add(new SlashCommandBuilder().setName(TEST_COMMAND).setDescription("[Placeholder]")
                    .setEnabledInDms(true));

        api.bulkOverwriteGlobalApplicationCommands(builders).join();
        System.out.println("Command registration complete");
    }

    private static String getHelpText() {
        return "Casino Bot Version " + Changelog.getVersion()
            + "\nCommands:"
            + "\n\t`/" + HELP_COMMAND + "` Displays this help text"
            + "\n\t`/" + CHANGELOG_COMMAND + "` View recent changes to the bot"
            + "\n\t`/" + LEADERBOARD_COMMAND + "` View the coin leaderboard"
            + "\n\t`/" + ROLL_COMMAND + "` Roll a random number."
            + "\n\t\tEither deathrolling (e.g. `100`) or RPG style dice (e.g. `1d20`)"
            + "\n\t`/" + CLAIM_COMMAND + "` Initialize yourself with some starting money"
            + "\n\t`/" + BALANCE_COMMAND + "` Check your balance"
            + "\n\t`/" + GIVE_COMMAND + "` Transfer money to someone else"
            + "\nIncome Commands:"
            + "\n\t`/" + WORK_COMMAND + "` Work for 2 hours to earn some coins"
            + "\n\t`/" + FISH_COMMAND + "` Fish for 30 minutes to earn some coins"
            + "\n\t`/" + ROB_COMMAND + "` Attempt to rob The Bank to steal some of The Money, you might be caught!"
            + "\n\t`/" + PICKPOCKET_COMMAND + "` Attempt a petty theft of pickpocketting"
            + "\nGambling Commands:"
            + "\n\t`/" + GUESS_COMMAND + "` Guess a number from 1 to 10"
            + "\n\t`/" + HUGEGUESS_COMMAND + "` Guess a number from 1 to 100"
            + "\n\t`/" + SLOTS_COMMAND + "` Spin the slots!"
            + "\n\t`/" + MINISLOTS_COMMAND + "` Spin the little slots!"
            + "\n\t`/" + POT_COMMAND + "` Check the Money Machine pot"
            + "\n\t`/" + FEED_COMMAND + "` Feed the Money Machine!"
            + "\n\t`/" + OVERUNDER_COMMAND + "` Multiple rounds of predicting if the next number is over or under"
            + "\n\t`/" + BLACKJACK_COMMAND + "` Play a hand of blackjack"
            + "\n\t`/" + ALLORNOTHING_COMMAND + "` Push your luck and go for a new record!"
            + "\nGacha Commands:"
            + "\n\t`/" + PULL_COMMAND + "` Pull for gacha characters!"
            + "\n\t`/" + PULLS_COMMAND + "` Check your available pulls"
            + "\n\t`/" + PITY_COMMAND + "` Check your gacha pity"
            + "\n\t`/" + GACHA_CHARACTER_LIST_COMMAND + "` List the characters you've pulled"
            + "\n\t`/" + GACHA_CHARACTER_INFO_COMMAND + "` View information about an owned character"
            + "\n\t`/" + GACHA_BANNER_LIST_COMMAND + "` List the available banners"
            + "\n\t`/" + GACHA_BANNER_INFO_COMMAND + "` View information about an available banner";
    }

    private static <T extends InteractionBase> void respondImmediately(SingleResponse singleResponse, T interaction) {
        respondImmediately(singleResponse, interaction, false);
    }

    private static <T extends InteractionBase> void respondImmediately(SingleResponse singleResponse, T interaction, boolean ephemeral) {
        InteractionImmediateResponseBuilder builder = interaction.createImmediateResponder().setContent(singleResponse.message);
        if (singleResponse.buttons != null) {
            builder = builder.addComponents(singleResponse.buttons);
        }
        if (ephemeral) {
            builder.setFlags(MessageFlag.EPHEMERAL);
        }
        builder.respond();
    }

    private static void makeMultiStepResponse(MultistepResponse multistepResponse,
            InteractionOriginalResponseUpdater updater) {
        if (updater == null) {
            throw new IllegalArgumentException("Updater was null");
        }
        if (!multistepResponse.hasMessages()) {
            return;
        }
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                boolean finished = false;
                if (!multistepResponse.next()) {
                    finished = true;
                } else {
                    updater.setContent(multistepResponse.getMessage());
                    if (multistepResponse.hasAttachment()) {
                        updater.addAttachment(multistepResponse.getAttachment());
                    }
                    finished = multistepResponse.isAtEnd();
                }

                if (finished) {
                    timer.cancel();
                    if (multistepResponse.buttons != null) {
                        updater.addComponents(multistepResponse.buttons);
                    }
                }

                updater.update();
            }
        }, 0, multistepResponse.delay);
    }

    static void sendMessage(EmbedResponse embedResponse, long channelID) {
        Optional<TextChannel> channel = api.getTextChannelById(channelID);
        if (!channel.isPresent()) {
            System.out.println("Failed to send message to channel " + channelID + ": Channel was not found");
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(embedResponse.getColor());
        embedBuilder.setDescription(embedResponse.getMessage());
        if (embedResponse.hasTitle()) {
            embedBuilder.setTitle(embedResponse.getTitle());
        }
        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.setEmbed(embedBuilder);
        if (embedResponse.hasButtons()) {
            messageBuilder.addComponents(embedResponse.getButtons());
        }
        messageBuilder.send(channel.get());
    }
}
