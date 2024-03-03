package com.c2t2s.hb;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.ActionRow;
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
import static java.util.Map.entry;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.Set;

public class HBMain {

    private static final String VERSION_STRING = "3.3.1.6"; //Update this in pom.xml too when updating
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

    private abstract static class CasinoCommand {
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

    static class SimpleCasinoCommand extends CasinoCommand {
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

    static class ImmediateCasinoCommand extends CasinoCommand {
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

    static class MultistepCasinoCommand extends CasinoCommand {
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
    private static final String TEST_COMMAND = "test";

    private static final long DEFAULT_LEADERBOARD_LENGTH = 3L;
    private static final long DEFAULT_CASINO_WAGER = 100L;
    private static final long DEFAULT_ALLORNOTHING_WAGER = 500L;
    private static final long DEFAULT_PULL_AMOUNT = 1L;

    private static Set<Long> casinoChannels = new HashSet<>();
    private static Set<Long> gachaChannels = new HashSet<>();
    private static Set<Long> adminUsers = new HashSet<>();
    private static Map<String, CasinoCommand> commands = Map.ofEntries(
            entry(VERSION_COMMAND, new SimpleCasinoCommand(
                () -> VERSION_STRING)),
            entry(HELP_COMMAND, new SimpleCasinoCommand(
                HBMain::getHelpText,
                true)),
            entry(CHANGELOG_COMMAND, new SimpleCasinoCommand(
                i -> getChangelog(i.getArgumentStringValueByIndex(0).orElse("")),
                true)),
            entry(LATEST_RELEASE_COMMAND, new SimpleCasinoCommand(
                HBMain::getLatestRelease)),
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
            entry(TEST_COMMAND, new SimpleCasinoCommand(
                Items::handleTest,
                false,
                false,
                false))
        );

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("API key is required as first argument");
            return;
        }

        DiscordApi api = new DiscordApiBuilder().setToken(args[0]).login().join();
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

            // Ensure user is allowed to run this command in this channel
            if (!(adminUsers.contains(interaction.getUser().getId())
                    || (command.isValidInCasinoChannels() && casinoChannels.contains(interaction.getChannel().get().getId()))
                    || (command.isValidInGachaChannels() && gachaChannels.contains(interaction.getChannel().get().getId())))) {
                respondImmediately(
                    new SingleResponse("Unable to run `" + interaction.getFullCommandName()
                        + "` in this channel. If this is unexpected, have an admin run `/registerchannel`"),
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
                Arrays.asList(SlashCommandOptionChoice.create(VERSION_STRING, VERSION_STRING),
                    SlashCommandOptionChoice.create("3.2.0-latest", "3.2.0-latest"),
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
        builders.add(new SlashCommandBuilder().setName(TEST_COMMAND).setDescription("[Placeholder]")
                    .setEnabledInDms(true));

        api.bulkOverwriteGlobalApplicationCommands(builders).join();
        System.out.println("Command registration complete");
    }

    private static String getHelpText() {
        return "Casino Bot Version " + VERSION_STRING
            + "\nCommands:"
            + "\n\t`/help` Displays this help text"
            + "\n\t`/changelog` View recent changes to the bot"
            + "\n\t`/roll` Roll a random number."
            + "\n\t\tEither deathrolling (e.g. `100`) or RPG style dice (e.g. `1d20`)"
            + "\n\t`/claim` Initialize yourself with some starting money"
            + "\n\t`/balance` Check your balance"
            + "\n\t`/richest` Check who's the richest"
            + "\n\t`/give` Transfer money to someone else"
            + "\nIncome Commands:"
            + "\n\t`/work` Work for 2 hours to earn some coins"
            + "\n\t`/fish` Fish for 30 minutes to earn some coins"
            + "\n\t`/rob` Attempt to rob The Bank to steal some of The Money, you might be caught!"
            + "\n\t`/pickpocket` Attempt a petty theft of pickpocketting"
            + "\nGambling Commands:"
            + "\n\t`/guess` Guess a number from 1 to 10"
            + "\n\t`/hugeguess` Guess a number from 1 to 100"
            + "\n\t`/slots` Spin the slots!"
            + "\n\t`/minislots` Spin the little slots!"
            + "\n\t`/pot` Check the Money Machine pot"
            + "\n\t`/feed` Feed the Money Machine!"
            + "\n\t`/overunder` Multiple rounds of predicting if the next number is over or under"
            + "\n\t\tStart a new game with `new`"
            + "\n\t\tPlace predictions with `over`, `under`, or `same`"
            + "\n\t`/blackjack` Play a hand of blackjack"
            + "\n\t`/allornothing` Push your luck and go for a new record!"
            + "\n\t\tStart or resume a game with `/allornothing`, play with the buttons"
            + "\nGacha Commands:"
            + "\n\t`/pull` Pull for gacha characters!"
            + "\n\t`/pulls` Check your available pulls"
            + "\n\t`/pity` Check your gacha pity"
            + "\n\t`/gacha character list` List the characters you've pulled"
            + "\n\t`/gacha character info` View information about an owned character"
            + "\n\t`/gacha banner list` List the available banners"
            + "\n\t`/gacha banner info` View information about an available banner";
    }

    private static String getLatestRelease() {
        return "Casino Bot Release " + VERSION_STRING + ":" + getLatestReleaseString();
    }

    private static String getLatestReleaseString() {
        return "\n- Fixed a case where gacha characters could be awarded from outside the selected banner"
            + "\n- Added a button to facilitate deathroll followups";
    }

    private static String getChangelog(String version) {
        switch (version) {
            default:
            case "3.2.0-latest":
                return "Changelog:\n" + VERSION_STRING + getLatestReleaseString()
                    + "\n3.3.0"
                    + "\n- Splitting is now supported in blackjack"
                    + "\n- Updated how blackjack stats are stored. This should now cause blackjack to no longer appear to massively "
                    + "underperform when viewing `/stats`, but existing blackjack stats have been reset as a result"
                    + "\n- Simplified the command for a new blackjack game to just `/blackjack` instead of `/blackjack new`"
                    + "\n3.2.0"
                    + "\n- After 4 years, the bot now has actual workout tracking functionality!"
                    + "\n- Added `/workout` to report you've completed a workout (or other activity)"
                    + "\n- Added `/selectworkoutreward` to configure the reward awarded from `/workout`"
                    + "\n- The output from these commands is visible only to you, so feel free to use them for any self-improvement activity of your choice (doesn't have to just be workouts)"
                    + "\n- Workout tracks the number of successive days you've completed workouts, but the tracking can be overwritten so you can control how streaks are tracked"
                    + "\n- Added the ability to view old changelogs via argument";
            case "3.1.8-3.1.11":
                return "Historical Changelog for 3.1.8-3.1.11:"
                    + "\n3.1.11"
                    + "\n- Added personalized stats to `/stats`. See how much you've ~~wasted~~ won in each game!"
                    + "\n- Improved the stat tracking for minislots. Minislot stats have been wiped as a result (there wasn't a ton there though)"
                    + "\n- Added new holiday characters to the gacha pool"
                    + "\n- Added a new Holiday banner containing the new characters as well as the existing Spooky ones"
                    + "\n- Reduced 3 win `/overunder` payout to 2.5x wager based on experimental payout rates (was overperforming by 13%)"
                    + "\n3.1.10"
                    + "\n- Migrated gacha pulling to be banner-based"
                    + "\n- Added `/gacha banner list` and `/gacha banner info` to view banner information"
                    + "\n- Added `/gacha character info` to view information about an owned character"
                    + "\n- Reduced coins awarded for characters already maxed, as they are easier to target via banners"
                    + "\n3.1.9"
                    + "\n- Fixed bug where it was sometime possible to get more gacha duplicates than intended"
                    + "\n- Gacha characters are now sorted by rarity, then alphabetically by name when listed"
                    + "\n3.1.8"
                    + "\n- Limit `/pull` to 25 pulls at once so response fits within a discord message"
                    + "\n- Adds `/allornothing` - push your luck and try to set a new record"
                    + "\n- Adds `/stats` to see game odds"
                    + "\n- Animated responses should respond slightly faster"
                    + "\n- Blackjack and overunder buttons will no longer be appended to some error messages"
                    + "\n- Character images are now attached to `/pull` output as attachments";
            case "3.1.0-3.1.7":
                return "Historical Changelog for 3.1.0-3.1.7:"
                    + "\n3.1.7"
                    + "\n- Gacha character pull rates are now increased at high pity"
                    + "\n- `/feed` now correctly increases the payout chance when the pot is large"
                    + "\n- `/pull` will no longer timeout when using many pulls at once"
                    + "\n- Default wager on all games is now 100"
                    + "\n- `/roll` should no properly handle negatives when using RPG style input"
                    + "\n3.1.6"
                    + "\n- Adds the abillity to perform multiple pulls at once"
                    + "\n3.1.5"
                    + "\n- First pull check after a user's daily reset will now correctly have the reset applied"
                    + "\n3.1.4"
                    + "\n- `/pulls` now lists available pull sources or remaining timer"
                    + "\n- Pity now remains unchanged when pulling a character of a higher rarity"
                    + "\n- Characters are now half as likely (1/4 -> 1/8 for 1 Stars, 1/16 -> 1/32 for 2 Stars, 1/64 -> 1/128 for 3 Stars)"
                    + "\n- Shiny Characters are now less likely (1/8 -> 1/20)"
                    + "\n- Test Character B has been temporarily disabled for balance reasons"
                    + "\n3.1.3"
                    + "\n- `/give` now pings the recipient"
                    + "\n- `/blackjack` now resolves incrementally"
                    + "\n3.1.2"
                    + "\n- Adds `/pity` and `/pulls`"
                    + "\n3.1.1"
                    + "\n- First 2h and 30m income command per day now award Gacha pulls"
                    + "\n3.1.0"
                    + "\n- Adds `/pull` to test the gacha system";
            case "2.0.0-2.0.13":
                return "Historical Changelog for 2.0.0-2.0.13:"
                    +"\n2.0.13:"
                    + "\n\t- Fix for `/blackjack hit` with no active game"
                    + "\n2.0.12"
                    + "\n- Add buttons to `/blackjack new` and `/overunder new`"
                    + "\n2.0.11"
                    + "\n- Formating fixes for help text"
                    + "\n2.0.10"
                    + "\n- Fixes for `/blackjack`, `/overunder`, and `/feed`"
                    + "\n2.0.9"
                    + "\n- Readd `/pot`, `/feed`, `/blackjack`, `/overunder`, and `/give`"
                    + "\n2.0.8"
                    + "\n- Update income command help prompts to reference slash commands"
                    + "\n2.0.7"
                    + "\n- Readd `/balance`, `/work`, `/fish`, `/rob`, `/pickpocket`"
                    + "\n2.0.6"
                    + "\n- Readd `/claim`"
                    + "\n- Readd full implementation of `/minislots`"
                    + "\n- Hook up DB to existing commands"
                    + "\n2.0.5"
                    + "\n- Update slots to update existing messages"
                    + "\n2.0.4"
                    + "\n- Readded `/slots` and `/minislots`, minislots is temporarily an alias of slots"
                    + "\n2.0.3"
                    + "\n- Added `/changelog`. Readded `/help`, `/roll`, and `/hugeguess`"
                    + "\n2.0.2"
                    + "\n- Fixed bot not responding to guesses with default wagers"
                    + "\n2.0.1"
                    + "\n- Readded `/guess`"
                    + "\n2.0.0"
                    + "\n- Bot is back! Added one sample command";
            case "1.5.0-1.5.5":
                return "Historical Changelog for 1.5.0-1.5.5:"
                    + "\n1.5.5:"
                    + "\n- Updated income command options with some new options"
                    + "\n1.5.4:"
                    + "\n- Another fix for incorrect wager payout calculations"
                    + "\n1.5.3:"
                    + "\n- Fixed first entry of +wagerinfo output always showing as 0"
                    + "\n- There are now no known issues with the wager system"
                    + "\n1.5.2:"
                    + "\n- Fixed wager payout"
                    + "\n1.5.0 & 1.5.1:"
                    + "\n- Adds wager and bet commands. Known issues: Payouts only go out to the first person, and wagerinfo total is off for the first option";
            case "1.4.0-1.4.8":
                return "Historical Changelog for 1.4.0-1.4.8:"
                    + "\n1.4.8:"
                    + "\n- Change 2 win overunder payout to 1:1 from 2:1 based on player win rates. The payouts may be adjusted again when more data is collected"
                    + "\n1.4.7:"
                    + "\n- Rolls 1.4.6 change out to games other than overunder"
                    + "\n- Fixes logging for overunder and blackjack winnings"
                    + "\n1.4.6:"
                    + "\n- For the purposes of the money machine, losses are now net losses"
                    + "\n1.4.5:"
                    + "\n- 5% of all casino losses will be added to the pot. The money machine no longer creates 100 coins when it pays out"
                    + "\n1.4.4:"
                    + "\n- Fix issue calculating blackjack totals when dealt an Ace"
                    + "\n1.4.3:"
                    + "\n- Fix crash for +blackjack with uninitialized users"
                    + "\n1.4.2:"
                    + "\n- Adjusts `+claim` to mention new games"
                    + "\n1.4.1:"
                    + "\n- Fixes issue where blackjack games wouldn't cleanup properly"
                    + "\n1.4.0:"
                    + "\n- Adds `+blackjack`, and associated `+hit` and `+stand`";
            case "1.0.0-1.3.2":
                return "Historical Changelog for 1.0.5-1.3.2:"
                    + "\n1.3.2:"
                    + "\n- Fixed issue where overunder would always count equal values as correct"
                    + "\n1.3.1:"
                    + "\n- Nerfed slots"
                    + "\n  3 of a kind payout 2:1 >>> 1.5:1"
                    + "\n1.3.0:"
                    + "\n- Rebalanced slots"
                    + "\n  5 of a kinds payout 150:1 >>> 30:1"
                    + "\n  4 of a kind payout 8:1 >>> 10:1"
                    + "\n  3 of a kind payout 1:1 >>> 2:1"
                    + "\n1.2.0:"
                    + "\n- Adds `+overunder` and associated `+over`, `+under`, and `+same`"
                    + "\n1.1.2:"
                    + "\n- Fixed a potential database error that could cause some commands to not return as expected"
                    + "\n1.1.1:"
                    + "\n- The money machine can no longer eat air"
                    + "\n1.1.0:"
                    + "\n- Added +moneymachine and +pot"
                    + "\n1.0.9:"
                    + "\n- Added +hugeguess, guess a number from 1 - 100"
                    + "\n1.0.8:"
                    + "\n- Slots nerfed:"
                    + "\n  Fruit salad payout 6.5:1 >>> 2:1"
                    + "\n1.0.7:"
                    + "\n- Fixes a gamebreaking bug where the cook emoji was chef instead"
                    + "\n1.0.6:"
                    + "\n- Added +bigguess, the classic 10:1 odds"
                    + "\n1.0.5:"
                    + "\n- Slots buffed:"
                    + "\n  5 of a kinds payout 8:1 >>> 150:1"
                    + "\n  4 of a kind payout 6:1 >>> 8:1"
                    + "\n  3 of a kind payout 0.5:1 >>> 1:1"
                    + "\n  Fruit salad payout 6:1 >>> 6.5:1"
                    + "\n1.0.4:"
                    + "\n- Fix pickpocket error text"
                    + "\n- Correctly apply guess nerf"
                    + "\n- Update +help"
                    + "\n1.0.3:"
                    + "\n- Nerf guess payout"
                    + "\n1.0.2:"
                    + "\n- Nerf guess payout at 1 and 10"
                    + "\n1.0.1:"
                    + "\n- Working and fishing now correctly award money"
                    + "\n- Minislots no longer calls slots"
                    + "\n- Guess now pays out more at 1 and 10"
                    + "\n1.0.0:"
                    + "\n- Revamp income commands";
        }
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
}
