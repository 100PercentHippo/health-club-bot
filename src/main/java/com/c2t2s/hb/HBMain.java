package com.c2t2s.hb;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.component.ButtonStyle;
import org.javacord.api.interaction.MessageComponentInteraction;
import org.javacord.api.interaction.InteractionBase;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class HBMain {

    private static final String version = "3.1.2"; //Update this in pom.xml too

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("API key is required as first argument");
            return;
        }
        DiscordApi api = new DiscordApiBuilder().setToken(args[0]).login().join();
        if (args.length > 1 && args[1].equalsIgnoreCase("init")) {
            initCommands(api);
        }
        api.addSlashCommandCreateListener(event -> {
            SlashCommandInteraction interaction = event.getSlashCommandInteraction();
            switch (interaction.getFullCommandName()) {
                case "version":
                    interaction.createImmediateResponder().setContent(version).respond();
                    break;
                case "help":
                    interaction.createImmediateResponder().setContent(getHelpText()).respond();
                    break;
                case "changelog":
                    interaction.createImmediateResponder().setContent(getChangelog()).respond();
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
                            interaction.getArgumentLongValueByIndex(1).orElse(10L))).respond();
                    break;
                case "hugeguess":
                    interaction.createImmediateResponder().setContent(
                        Casino.handleHugeGuess(interaction.getUser().getId(),
                            interaction.getArgumentLongValueByIndex(0).get(),
                            interaction.getArgumentLongValueByIndex(1).orElse(10L))).respond();
                    break;
                case "slots":
                    makeMultiStepResponse(
                        Casino.handleSlots(interaction.getUser().getId(), interaction.getArgumentLongValueByIndex(0).orElse(10L)),
                        1000, interaction);
                    break;
                case "minislots":
                    makeMultiStepResponse(
                        Casino.handleMinislots(interaction.getUser().getId(), interaction.getArgumentLongValueByIndex(0).orElse(10L)),
                        1000, interaction);
                    break;
                case "overunder new":
                    interaction.createImmediateResponder().setContent(
                        Casino.handleOverUnderInitial(interaction.getUser().getId(), interaction.getArgumentLongValueByIndex(0).orElse(10L)))
                        .addComponents(ActionRow.of(Button.secondary("overunder.over", "Over"),
                            Button.secondary("overunder.under", "Under"),
                            Button.secondary("overunder.same", "Same")))
                        .respond();
                    break;
                case "overunder over":
                    interaction.createImmediateResponder().setContent(
                        Casino.handleOverUnderFollowup(interaction.getUser().getId(), Casino.PREDICTION_OVER)).respond();
                    break;
                case "overunder under":
                    interaction.createImmediateResponder().setContent(
                        Casino.handleOverUnderFollowup(interaction.getUser().getId(), Casino.PREDICTION_UNDER)).respond();
                    break;
                case "overunder same":
                    interaction.createImmediateResponder().setContent(
                        Casino.handleOverUnderFollowup(interaction.getUser().getId(), Casino.PREDICTION_SAME)).respond();
                    break;
                case "blackjack new":
                    interaction.createImmediateResponder().setContent(
                        Blackjack.handleBlackjack(interaction.getUser().getId(), interaction.getArgumentLongValueByIndex(0).orElse(10L)))
                        .addComponents(ActionRow.of(Button.secondary("blackjack.hit", "Hit"),
                            Button.secondary("blackjack.stand", "Stand")))
                        .respond();
                    break;
                case "blackjack hit":
                    interaction.createImmediateResponder().setContent(
                        Blackjack.handleHit(interaction.getUser().getId())).respond();
                    break;
                case "blackjack stand":
                	makeMultiStepResponse(
                		Blackjack.handleStand(interaction.getUser().getId()), 1000, interaction);
                    break;
                case "pull":
                	makeMultiStepResponse(
                		Gacha.handleGachaPull(interaction.getUser().getId(), false), 1000, interaction);
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
            }
        });
        api.addMessageComponentCreateListener(event -> {
            MessageComponentInteraction interaction = event.getMessageComponentInteraction();
            String response = "";
            int prediction = 0;
            if (interaction.getCustomId().contains("overunder")) {
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
                }
                response = Casino.handleOverUnderFollowup(interaction.getUser().getId(), prediction);
                if (response.contains("balance")) {
                    interaction.createImmediateResponder().setContent(response).respond();
                } else {
                    interaction.createImmediateResponder().setContent(response)
                    .addComponents(ActionRow.of(Button.secondary("overunder.over", "Over"),
                        Button.secondary("overunder.under", "Under"),
                        Button.secondary("overunder.same", "Same")))
                    .respond();
                }
            } else if (interaction.getCustomId().contains("blackjack")) {
                if (interaction.getCustomId().equals("blackjack.hit")) {
                    response = Blackjack.handleHit(interaction.getUser().getId());
                    if (response.contains("balance")) {
                        interaction.createImmediateResponder().setContent(response).respond();
                    } else {
                        interaction.createImmediateResponder().setContent(response)
                        .addComponents(ActionRow.of(Button.secondary("blackjack.hit", "Hit"),
                            Button.secondary("blackjack.stand", "Stand")))
                        .respond();
                    }
                } else if (interaction.getCustomId().equals("blackjack.stand")) {
                	makeMultiStepResponse(Blackjack.handleStand(interaction.getUser().getId()),
                		1000, interaction);
                }
            }
        });
    }
    
    private static void initCommands(DiscordApi api) {
        System.out.println("Registering commands with discord");
        //SlashCommand.with("version", "Check the current bot version").createGlobal(api).join();
        //SlashCommand.with("help", "Print available Casino Bot commands").createGlobal(api).join();
        //SlashCommand.with("changelog", "Print recent Casino Bot changelog").createGlobal(api).join();
        //SlashCommand.with("roll", "Roll a random number. Supports deathrolling (`/roll 10`) or RPG style dice (`/roll 1d20`)",
        //    Arrays.asList(SlashCommandOption.createStringOption("argument", "What to roll. Either a number (`100`) or an RPG style sequence (`1d20`)", true)))
        //    .createGlobal(api).join();
        //SlashCommand.with("guess", "Guess a number from 1 to 10!",
        //    Arrays.asList(SlashCommandOption.createLongOption("guess", "What you think the number will be", true, 1, 10),
        //        SlashCommandOption.createLongOption("wager", "Amount to wager, default 10", false, 1, 100000)))
        //    .createGlobal(api).join();
        //SlashCommand.with("hugeguess", "Guess a number from 1 to 100!",
        //    Arrays.asList(SlashCommandOption.createLongOption("guess", "What you think the number will be", true, 1, 100),
        //        SlashCommandOption.createLongOption("wager", "Amount to wager, default 10", false, 1, 100000)))
        //    .createGlobal(api).join();
        // SlashCommand.with("slots", "Spin the slots!",
        //     Arrays.asList(SlashCommandOption.createLongOption("wager", "Amount to wager, default 10", false, 1, 100000)))
        //     .setEnabledInDms(false).createGlobal(api).join();
        // SlashCommand.with("minislots", "Spin the little slots!",
        //     Arrays.asList(SlashCommandOption.createLongOption("wager", "Amount to wager, default 10", false, 1, 100000)))
        //     .setEnabledInDms(false).createGlobal(api).join();
        //SlashCommand.with("claim", "Initialize yourself as a casino user").setEnabledInDms(false).createGlobal(api).join();
        // SlashCommand.with("balance", "Check your current balance").setEnabledInDms(false).createGlobal(api).join();
        // SlashCommand.with("work", "Work for 2 hours to earn some coins").setEnabledInDms(false).createGlobal(api).join();
        // SlashCommand.with("fish", "Fish for 30 minutes to earn some coins").setEnabledInDms(false).createGlobal(api).join();
        // SlashCommand.with("rob", "Attempt to rob The Bank to steal some of The Money. You might be caught!")
        //     .setEnabledInDms(false).createGlobal(api).join();
        // SlashCommand.with("pickpocket", "Attempt a petty theft of pickpocketting").setEnabledInDms(false).createGlobal(api).join();
        // SlashCommand.with("leaderboard", "View the richest people in the casino",
        //     Arrays.asList(SlashCommandOption.createLongOption("entries", "Number of entries to show, default 3", true, 1, 10)))
        //     .setEnabledInDms(false).createGlobal(api).join();
        // SlashCommand.with("richest", "View the richest people in the casino",
        //     Arrays.asList(SlashCommandOption.createLongOption("entries", "Number of entries to show, default 3", true, 1, 10)))
        //     .setEnabledInDms(false).createGlobal(api).join();
        // SlashCommand.with("pot", "Check how much money is in the Money Machine")
        //     .setEnabledInDms(false).createGlobal(api).join();
        // SlashCommand.with("feed", "Feed the Money Machine",
        //     Arrays.asList(SlashCommandOption.createLongOption("amount", "How much to feed", true, 1, 100000)))
        //     .setEnabledInDms(false).createGlobal(api).join();
        // SlashCommand.with("overunder", "Multiple rounds of predicting if the next number is over or under",
        //     Arrays.asList(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "new", "Begin a new game of over-under",
        //         Arrays.asList(SlashCommandOption.createLongOption("wager", "Amount to wager, default 10", false, 1, 100000))),
        //         SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "over", "Guess the next number in an ongoing game will be over"),
        //         SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "under", "Guess the next number in an ongoing game will be under"),
        //         SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "same", "Guess the next number in an ongoing game will be the same")))
        //     .setEnabledInDms(false).createGlobal(api).join();
        // SlashCommand.with("blackjack", "Play a game of blackjack",
        //     Arrays.asList(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "new", "Begin a new game of blackjack",
        //         Arrays.asList(SlashCommandOption.createLongOption("wager", "Amount to wager, default 10", false, 1, 100000))),
        //         SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "hit", "Ask the dealer for another card"),
        //         SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "stand", "Stand with the cards you have")))
        //     .setEnabledInDms(false).createGlobal(api).join();
        // SlashCommand.with("give", "Give coins to another user",
        //     Arrays.asList(SlashCommandOption.createUserOption("recipient", "Person to give coins to", true),
        //         SlashCommandOption.createLongOption("amount", "Amount to transfer", true)))
        //     .setEnabledInDms(false).createGlobal(api).join();
        // SlashCommand.with("pull", "Try to win a gacha character!")
        //      .setEnabledInDms(false).createGlobal(api).join();
        // SlashCommand.with("pulls", "Check how many gacha pulls you have")
        //      .setEnabledInDms(false).createGlobal(api).join();
        // SlashCommand.with("pity", "Check your gacha pity")
        //      .setEnabledInDms(false).createGlobal(api).join();
        // SlashCommand.with("gacha", "Ha! Gotcha!",
        //     Arrays.asList(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND_GROUP, "character", "Interact with your characters",
        //         Arrays.asList(SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "list", "List the characters you've got")))))
        //     .setEnabledInDms(false).createGlobal(api).join();
        // TODO: Update leaderboard/richest's argument to be optional
        // TODO: Create /blackjack and /overunder as aliases to start new games
        System.out.println("Command registration complete");
    }

    private static String getHelpText() {
        return "Casino Bot Verstion " + version
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
            + "\nGacha Commands:"
            + "\n\t`/pull` Pull for gacha characters!"
            + "\n\t`/pulls` Check your available pulls"
            + "\n\t`/pity` Check your gacha pity"
            + "\n\t`/gacha character list` List the characters you've pulled";
    }

    private static String getChangelog() {
        return "3.1.2"
        	+ "\n\tAdds `/pity` and `/pulls`"
        	+ "\n3.1.1"
        	+ "\n\tFirst 2h and 30m income command per day now award Gacha pulls"
        	+ "\n3.1.0"
        	+ "\n\tAdds `/pull` to test the gacha system"
        	+ "\n3.0.2"
        	+ "\n\tCorrects wager limits for blackjack and overunder"
        	+ "\n\tRemoves automatic contributions to the money machine from casino net profits"
        	+ "\n\tMoney machine now instead retains 25% of the pot when paying out"
        	+ "\n3.0.1"
        	+ "\n\t`/feed` is once again working"
        	+ "\n3.0.0"
        	+ "\n\tBot is back (again)!";
    }

    //TODO: Handle negative modifiers in dice rolls
    private static String handleRoll(String args) {
        int max = 0;
        Random random = new Random();
        try {
            if (args.contains("d")) {
                //Dice rolling
                //args.replace("-\\s*-", "");
                args.replace("-", "+-");
                args.replace("\\s", "");
                String[] pieces = args.split("\\+");
                String message = "";
                int total = 0;
                for (int i = 0; i < pieces.length; ++i) {
                    boolean negative = false;
                    if (pieces[i].startsWith("-")) {
                        pieces[i] = pieces[i].substring(1);
                        negative = true;
                    }
                    if (!pieces[i].contains("d")) {
                        int roll = Integer.parseInt(pieces[i]);
                        message += ((negative ? " - " : " + ") + roll);
                        total += (negative ? -1 : 1) * roll;
                        continue;
                    }
                    String[] splitArgs = pieces[i].split("d");
                    // If a NumberFormatException occurs, pass it up, don't catch
                    int numDice = Integer.parseInt(splitArgs[0]);
                    int diceSize = Integer.parseInt(splitArgs[1]);
                    String text = "";
                    for (int j = 0; j < numDice; ++j) {
                        int roll = random.nextInt(diceSize) + 1;
                        total += (roll * (negative ? -1 : 1));
                        text += (negative ? " - " : " + ") + "`" + roll + "`";
                    }
                    text = text.substring(2, text.length());
                    if (message.length() != 0) {
                        message += (negative ? " - " : " + ");
                    }
                    message += text;
                }
                return message + "\n`" + total + "`";
            } else {
                // Deathrolling
                max = Integer.parseInt(args);
                if (max < 1) {
                    return "Negative numbers make me sad :slight_frown:";
                }
                int roll = random.nextInt(max) + 1;
                return "" + roll + (roll == 1 ? "\nIt's been a pleasure doing business with you :slight_smile: :moneybag:" : "");
            }
        } catch (NumberFormatException e) {
            // Unrecognized syntax
            return "Unrecognized roll syntax. Try `/roll 3` or `/roll 2d6`";
        }
    }

    private static void makeMultiStepResponse(List<String> responseSteps, long delay /* milliseconds */, InteractionBase interaction) {
        CompletableFuture<InteractionOriginalResponseUpdater> updater
            =  interaction.createImmediateResponder().setContent(responseSteps.remove(0)).respond();
        if (responseSteps.size() > 0) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        updater.get().setContent(responseSteps.remove(0)).update();
                    } catch (ExecutionException | InterruptedException e) {
                        System.out.println("Exception while updating delayed message:");
                        e.printStackTrace();
                    }
                    if (responseSteps.size() == 0) {
                        timer.cancel();
                    }
                }
            }, delay, delay);
        }
    }
}
