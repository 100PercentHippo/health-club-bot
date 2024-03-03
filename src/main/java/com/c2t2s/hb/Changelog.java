package com.c2t2s.hb;

class Changelog {

    // Hide default constructor
    private Changelog() {}

    private static final String VERSION_STRING = "3.3.1.6"; //Update this in pom.xml too when updating

    static String getVersion() {
        return VERSION_STRING;
    }

    static String getLatestRelease() {
        return "Casino Bot Release " + VERSION_STRING + ":" + getLatestReleaseString();
    }

    private static String getLatestReleaseString() {
        return "\n- Fixed a case where gacha characters could be awarded from outside the selected banner"
            + "\n- Added a button to facilitate deathroll followups";
    }

    static String getChangelog(String version) {
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

}