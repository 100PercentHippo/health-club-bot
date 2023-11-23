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
import org.javacord.api.interaction.SlashCommandOptionBuilder;
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
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Set;

public class HBMain {

    private static final String VERSION_STRING = "3.1.9"; //Update this in pom.xml too when updating
    static final Random RNG_SOURCE = new Random();

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

        void setMessage(String message) {
            this.message = message;
        }

        void setButtons(ActionRow buttons) {
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
            switch (interaction.getFullCommandName()) {
                case "version":
                    interaction.createImmediateResponder().setContent(VERSION_STRING).respond();
                    break;
                case "help":
                    interaction.createImmediateResponder().setContent(getHelpText()).setFlags(MessageFlag.EPHEMERAL).respond();
                    break;
                case "changelog":
                    interaction.createImmediateResponder().setContent(getChangelog()).respond();
                    break;
                case "latestrelease":
                    interaction.createImmediateResponder().setContent(getLatestRelease()).respond();
                    break;
                case "roll":
                    interaction.createImmediateResponder().setContent(handleRoll(interaction.getArgumentStringValueByIndex(0).get())).respond();
                    break;
                case "claim":
                    interaction.createImmediateResponder().setContent(
                        Casino.handleClaim(interaction.getUser().getId(), interaction.getUser().getDiscriminatedName()))
                        .respond();
                    break;
                case "balance":
                    interaction.createImmediateResponder().setContent(Casino.handleBalance(interaction.getUser().getId())).respond();
                    break;
                case "leaderboard":
                case "richest":
                    interaction.createImmediateResponder().setContent(
                        Casino.handleLeaderboard(interaction.getArgumentLongValueByIndex(0).orElse(3L))).respond();
                    break;
                case "give":
                    interaction.createImmediateResponder().setContent(
                        Casino.handleGive(interaction.getUser().getId(), interaction.getArgumentUserValueByIndex(0).get().getId(),
                            interaction.getArgumentLongValueByIndex(1).get())).respond();
                    break;
                case "pot":
                    interaction.createImmediateResponder().setContent(Casino.handlePot()).respond();
                    break;
                case "feed":
                    interaction.createImmediateResponder().setContent(
                        Casino.handleFeed(interaction.getUser().getId(), interaction.getArgumentLongValueByIndex(0).get())).respond();
                    break;
                case "work":
                    interaction.createImmediateResponder().setContent(Casino.handleWork(interaction.getUser().getId())).respond();
                    break;
                case "fish":
                    interaction.createImmediateResponder().setContent(Casino.handleFish(interaction.getUser().getId())).respond();
                    break;
                case "rob":
                    interaction.createImmediateResponder().setContent(Casino.handleRob(interaction.getUser().getId())).respond();
                    break;
                case "pickpocket":
                    interaction.createImmediateResponder().setContent(Casino.handlePickpocket(interaction.getUser().getId())).respond();
                    break;
                case "guess":
                    interaction.createImmediateResponder().setContent(
                        Casino.handleGuess(interaction.getUser().getId(),
                            interaction.getArgumentLongValueByIndex(0).get(),
                            interaction.getArgumentLongValueByIndex(1).orElse(100L))).respond();
                    break;
                case "hugeguess":
                    interaction.createImmediateResponder().setContent(
                        Casino.handleHugeGuess(interaction.getUser().getId(),
                            interaction.getArgumentLongValueByIndex(0).get(),
                            interaction.getArgumentLongValueByIndex(1).orElse(100L))).respond();
                    break;
                case "slots":
                    interaction.respondLater().thenAccept(updater -> {
                        makeMultiStepResponse(Casino.handleSlots(interaction.getUser().getId(),
                            interaction.getArgumentLongValueByIndex(0).orElse(100L)),
                        updater);
                    });
                    break;
                case "minislots":
                    interaction.respondLater().thenAccept(updater -> {
                        makeMultiStepResponse(Casino.handleMinislots(interaction.getUser().getId(),
                            interaction.getArgumentLongValueByIndex(0).orElse(100L)),
                        updater);
                    });
                    break;
                case "overunder new":
                    respondImmediately(Casino.handleOverUnderInitial(interaction.getUser().getId(),
                        interaction.getArgumentLongValueByIndex(0).orElse(100L)),
                        interaction);
                    break;
                case "overunder over":
                    respondImmediately(Casino.handleOverUnderFollowup(interaction.getUser().getId(), Casino.PREDICTION_OVER),
                        interaction);
                    break;
                case "overunder under":
                    respondImmediately(Casino.handleOverUnderFollowup(interaction.getUser().getId(), Casino.PREDICTION_UNDER),
                        interaction);
                    break;
                case "overunder same":
                    respondImmediately(Casino.handleOverUnderFollowup(interaction.getUser().getId(), Casino.PREDICTION_SAME),
                        interaction);
                    break;
                case "blackjack new":
                    respondImmediately(Blackjack.handleBlackjack(interaction.getUser().getId(),
                        interaction.getArgumentLongValueByIndex(0).orElse(100L)),
                        interaction);
                    break;
                case "blackjack hit":
                    respondImmediately(Blackjack.handleHit(interaction.getUser().getId()),
                        interaction);
                    break;
                case "blackjack stand":
                    interaction.respondLater().thenAccept(updater -> {
                        makeMultiStepResponse(Blackjack.handleStand(interaction.getUser().getId()),
                        updater);
                    });
                    break;
                case "pull":
                    interaction.respondLater().thenAccept(updater -> {
                        makeMultiStepResponse(Gacha.handleGachaPull(interaction.getUser().getId(), false,
                            interaction.getArgumentLongValueByIndex(0).orElse(1L)),
                        updater);
                    });
                    break;
                case "gacha character list":
                    interaction.createImmediateResponder().setContent(
                        Gacha.handleCharacterList(interaction.getUser().getId())).respond();
                    break;
                case "pulls":
                    interaction.createImmediateResponder().setContent(
                        Gacha.handlePulls(interaction.getUser().getId())).respond();
                    break;
                case "pity":
                    interaction.createImmediateResponder().setContent(
                        Gacha.handlePity(interaction.getUser().getId())).respond();
                    break;
                case "stats":
                    interaction.createImmediateResponder().setContent(
                        Stats.handleStats(interaction.getArgumentStringValueByIndex(0).orElse(""),
                        interaction.getUser().getId())).respond();
                    break;
                case "allornothing":
                    interaction.respondLater().thenAccept(updater -> {
                        makeMultiStepResponse(AllOrNothing.handleNew(interaction.getUser().getId(),
                            interaction.getArgumentLongValueByIndex(0).get(),
                            interaction.getArgumentLongValueByIndex(1).orElse(500L)),
                        updater);
                    });
                    break;
                default:
                    return;
            }
        });
        api.addMessageComponentCreateListener(event -> {
            MessageComponentInteraction interaction = event.getMessageComponentInteraction();
            String prefix = interaction.getCustomId();
            int seperator = prefix.indexOf('.');
            if (seperator > 0) {
                prefix = prefix.substring(0, seperator);
            }
            switch (prefix) {
                case "overunder":
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
                    break;
                case "blackjack":
                    if (interaction.getCustomId().equals("blackjack.hit")) {
                        respondImmediately(Blackjack.handleHit(interaction.getUser().getId()), interaction);
                    } else if (interaction.getCustomId().equals("blackjack.stand")) {
                        interaction.respondLater().thenAccept(updater -> {
                            makeMultiStepResponse(Blackjack.handleStand(interaction.getUser().getId()),
                            updater);
                        });
                    }
                    break;
                case "allornothing":
                    String[] parts = interaction.getCustomId().split("\\|");
                    if (parts.length < 1) {
                        System.out.println("Encountered unexpected allornothing interaction: "
                            + interaction.getCustomId() + " (split into " + parts.toString() + ")");
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
                            interaction.respondLater().thenAccept(updater -> {
                                makeMultiStepResponse(AllOrNothing.handleRoll(interaction.getUser().getId(), rollsToDouble),
                                updater);
                            });
                            break;
                        default:
                            System.out.println("Encountered unexpected allornothing command: "
                                + command + " (full command " + interaction.getCustomId() + ")");
                            return;
                    }
                    break;
                default:
                    System.out.println("Encountered unexpected interaction prefix: " + prefix + "\nFull id: " + interaction.getCustomId());
            }
        });
        api.addAutocompleteCreateListener(event -> {
            AutocompleteInteraction interaction = event.getAutocompleteInteraction();
            switch (interaction.getFullCommandName()) {
                case "stats":
                    List<SlashCommandOptionChoice> choices = new ArrayList<>();
                    Arrays.stream(Stats.StatsOption.values())
                        .forEach(o -> choices.add(SlashCommandOptionChoice.create(o.getDescription(), o.getName())));
                    interaction.respondWithChoices(choices);
                    break;
                default:
                    return;
            }
        });
        System.out.println("Server started");
    }

    private static void initCommands(DiscordApi api) {
        System.out.println("Registering commands with discord");
        Set<SlashCommandBuilder> builders = new HashSet<>();

        builders.add(new SlashCommandBuilder().setName("version")
            .setDescription("Check the current bot version").setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("help")
            .setDescription("Print available Casino Bot commands").setEnabledInDms(true));
        builders.add(new SlashCommandBuilder().setName("changelog")
            .setDescription("Print recent Casino Bot changelog").setEnabledInDms(true));
        builders.add(new SlashCommandBuilder().setName("latestrelease")
            .setDescription("Print the changelog of the most recent Casino Bot update").setEnabledInDms(true));
        builders.add(new SlashCommandBuilder().setName("roll")
            .setDescription("Roll a random number. Supports deathrolling (`/roll 10`) or RPG style dice (`/roll 1d20`)")
            .addOption(SlashCommandOption.createStringOption("argument", "What to roll. Either a number (`100`) or an RPG style sequence (`1d20`)", true))
            .setEnabledInDms(true));
        builders.add(new SlashCommandBuilder().setName("guess").setDescription("Guess a number from 1 to 10!")
            .addOption(SlashCommandOption.createLongOption("guess", "What you think the number will be", true, 1, 10))
            .addOption(SlashCommandOption.createLongOption("wager", "Amount to wager, default 100", false, 1, 100000))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("hugeguess").setDescription("Guess a number from 1 to 100!")
            .addOption(SlashCommandOption.createLongOption("guess", "What you think the number will be", true, 1, 100))
            .addOption(SlashCommandOption.createLongOption("wager", "Amount to wager, default 100", false, 1, 100000))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("slots").setDescription("Spin the slots!")
            .addOption(SlashCommandOption.createLongOption("wager", "Amount to wager, default 100", false, 1, 100000))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("minislots").setDescription("Spin the little slots!")
            .addOption(SlashCommandOption.createLongOption("wager", "Amount to wager, default 100", false, 1, 100000))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("claim").setDescription("Initialize yourself as a casino user")
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("balance").setDescription("Check your current balance")
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("work").setDescription("Work for 2 hours to earn some coins")
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("fish").setDescription("Fish for 30 minutes to earn some coins")
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("rob").setDescription("Attempt to rob The Bank to steal some of The Money. You might be caught!")
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("pickpocket").setDescription("Attempt a petty theft of pickpocketting")
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("leaderboard").setDescription("View the richest people in the casino")
            .addOption(SlashCommandOption.createLongOption("entries", "Number of entries to show, default 3", false, 1, 10))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("richest").setDescription("View the richest people in the casino")
            .addOption(SlashCommandOption.createLongOption("entries", "Number of entries to show, default 3", false, 1, 10))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("pot").setDescription("Check how much money is in the Money Machine")
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("feed").setDescription("Feed the Money Machine")
            .addOption(SlashCommandOption.createLongOption("amount", "How much to feed", true, 1, 100000))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("overunder").setDescription("Multiple rounds of predicting if the next number is over or under")
            .addOption(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "new", "Begin a new game of over-under",
                Arrays.asList(SlashCommandOption.createLongOption("wager", "Amount to wager, default 100", false, 1, 100000))))
            .addOption(SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "over", "Guess the next number in an ongoing game will be over"))
            .addOption(SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "under", "Guess the next number in an ongoing game will be under"))
            .addOption(SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "same", "Guess the next number in an ongoing game will be the same"))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("blackjack").setDescription("Play a game of blackjack")
            .addOption(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "new", "Begin a new game of blackjack",
                Arrays.asList(SlashCommandOption.createLongOption("wager", "Amount to wager, default 100", false, 1, 100000))))
            .addOption(SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "hit", "Ask the dealer for another card"))
            .addOption(SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "stand", "Stand with the cards you have"))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("give").setDescription("Give coins to another user")
            .addOption(SlashCommandOption.createUserOption("recipient", "Person to give coins to", true))
            .addOption(SlashCommandOption.createLongOption("amount", "Amount to transfer", true, 1, 100000))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("pull").setDescription("Try to win a gacha character!")
            .addOption(SlashCommandOption.createLongOption("pulls", "Number of pulls to use, default 1", false, 1, 25))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("pulls").setDescription("Check how many gacha pulls you have")
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("pity").setDescription("Check your gacha pity")
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("gacha").setDescription("Character management commands")
            .addOption(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND_GROUP, "character", "Interact with your characters",
                Arrays.asList(SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "list", "List the characters you've got"))))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("allornothing").setDescription("Test your luck, and maybe set a high score")
            .addOption(SlashCommandOption.createWithChoices(SlashCommandOptionType.LONG, "odds", "Chance to win each roll", true,
                Arrays.asList(SlashCommandOptionChoice.create("70%", 2), SlashCommandOptionChoice.create("80%", 3),
                    SlashCommandOptionChoice.create("90%", 7))))
            .addOption(SlashCommandOption.createLongOption("wager", "Amount to wager, default 500", false, 500, 100000))
            .setEnabledInDms(false));
        builders.add(new SlashCommandBuilder().setName("stats").setDescription("Check the odds of a given game")
            .addOption(SlashCommandOption.createStringOption("Game", "Which game to display associated stats", true, true))
            .setEnabledInDms(false));

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
            + "\n\t\tStart a game with `/blackjack new`, play with `/blackjack hit` and `/blackjack stand`"
            + "\n\t`/allornothing` Push your luck and go for a new record!"
            + "\n\t\tStart or resume a game with `/allornothing`, play with the buttons"
            + "\nGacha Commands:"
            + "\n\t`/pull` Pull for gacha characters!"
            + "\n\t`/pulls` Check your available pulls"
            + "\n\t`/pity` Check your gacha pity"
            + "\n\t`/gacha character list` List the characters you've pulled";
    }

    private static String getLatestRelease() {
        return "Casino Bot Release " + VERSION_STRING + ":"
            + getLatestReleaseString();
    }

    private static String getLatestReleaseString() {
        return "\n- Fixed bug where it was sometime possible to get more gacha duplicates than intended"
            + "\n- Gacha characters are now sorted by rarity, then alphabetically by name when listed";
    }

    private static String getChangelog() {
        return "Changelog:\n" + VERSION_STRING
            + getLatestReleaseString()
            + "\n3.1.8"
            + "\n- Limit `/pull` to 25 pulls at once so response fits within a discord message"
            + "\n- Adds `/allornothing` - push your luck and try to set a new record"
            + "\n- Adds `/stats` to see game odds"
            + "\n- Animated responses should respond slightly faster"
            + "\n- Blackjack and overunder buttons will no longer be appended to some error messages"
            + "\n- Character images are now attached to `/pull` output as attachments"
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
    }

    //TODO: Handle negative modifiers in dice rolls
    private static String handleRoll(String args) {
        int max = 0;
        try {
            if (args.contains("d")) {
                //Dice rolling
                StringBuilder message = new StringBuilder("Rolling `" + args + "`\n");
                args = args.replace("-\\s*-", "");
                args = args.replace("-", "+-");
                args = args.replace("\\s", "");
                String[] pieces = args.split("\\+");
                int total = 0;
                for (int i = 0; i < pieces.length; ++i) {
                    boolean negative = false;
                    if (pieces[i].startsWith("-")) {
                        pieces[i] = pieces[i].substring(1);
                        negative = true;
                    }
                    if (!pieces[i].contains("d")) {
                        int roll = Integer.parseInt(pieces[i]);
                        message.append((negative ? " - " : " + ") + roll);
                        total += (negative ? -1 : 1) * roll;
                        continue;
                    }
                    String[] splitArgs = pieces[i].split("d");
                    // If a NumberFormatException occurs, pass it up, don't catch
                    int numDice = Integer.parseInt(splitArgs[0]);
                    int diceSize = Integer.parseInt(splitArgs[1]);
                    String text = "";
                    for (int j = 0; j < numDice; ++j) {
                        int roll = RNG_SOURCE.nextInt(diceSize) + 1;
                        total += (roll * (negative ? -1 : 1));
                        text += (negative ? " - " : " + ") + "`" + roll + "`";
                    }
                    text = text.substring(2, text.length());
                    if (message.length() != 0) {
                        message.append(negative ? " - " : " + ");
                    }
                    message.append(text);
                }
                return message.toString() + "\n`" + total + "`";
            } else {
                // Deathrolling
                max = Integer.parseInt(args);
                if (max < 1) {
                    return "Negative numbers make me sad :slight_frown:";
                }
                int roll = RNG_SOURCE.nextInt(max) + 1;
                return "Rolling 1-" + max + "\n" + roll
                    + (roll == 1 ? "\nIt's been a pleasure doing business with you :slight_smile: :moneybag:" : "");
            }
        } catch (NumberFormatException e) {
            // Unrecognized syntax
            return "Unrecognized roll syntax. Try `/roll 3` or `/roll 2d6`";
        }
    }

    private static <T extends InteractionBase> void respondImmediately(SingleResponse singleResponse, T interaction) {
        InteractionImmediateResponseBuilder builder = interaction.createImmediateResponder().setContent(singleResponse.message);
        if (singleResponse.buttons != null) {
            builder = builder.addComponents(singleResponse.buttons);
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
