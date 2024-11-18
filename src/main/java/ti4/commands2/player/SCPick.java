package ti4.commands2.player;

import java.util.Collection;
import java.util.Set;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.commands2.GameStateSubcommand;
import ti4.helpers.Constants;
import ti4.map.Game;
import ti4.map.Player;
import ti4.message.MessageHelper;
import ti4.service.strategycard.StrategyCardPickService;

public class SCPick extends GameStateSubcommand {

    public SCPick() {
        super(Constants.SC_PICK, "Pick a Strategy Card", true, true);
        addOptions(new OptionData(OptionType.INTEGER, Constants.STRATEGY_CARD, "Strategy Card #").setRequired(true));
        addOptions(new OptionData(OptionType.INTEGER, Constants.SC2, "2nd choice"));
        addOptions(new OptionData(OptionType.INTEGER, Constants.SC3, "3rd"));
        addOptions(new OptionData(OptionType.INTEGER, Constants.SC4, "4th"));
        addOptions(new OptionData(OptionType.INTEGER, Constants.SC5, "5th"));
        addOptions(new OptionData(OptionType.INTEGER, Constants.SC6, "6th"));
        addOptions(new OptionData(OptionType.STRING, Constants.FACTION_COLOR, "Faction or Color for which you set stats").setAutoComplete(true));

    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Game game = getGame();

        Collection<Player> activePlayers = game.getRealPlayers();
        if (activePlayers.isEmpty()) {
            MessageHelper.sendMessageToEventChannel(event, "No active players found");
            return;
        }

        int maxSCsPerPlayer = game.getStrategyCardsPerPlayer();
        if (maxSCsPerPlayer <= 0) maxSCsPerPlayer = 1;

        Player player = getPlayer();
        int playerSCCount = player.getSCs().size();
        if (playerSCCount >= maxSCsPerPlayer) {
            MessageHelper.sendMessageToEventChannel(event, "Player may not pick another strategy card. Max strategy cards per player for this game is " + maxSCsPerPlayer + ".");
            return;
        }

        OptionMapping option = event.getOption(Constants.STRATEGY_CARD);
        int scPicked = option.getAsInt();

        boolean pickSuccessful = Stats.pickSC(event, game, player, option);
        Set<Integer> playerSCs = player.getSCs();
        if (!pickSuccessful) {
            if (game.isFowMode()) {
                String[] scs = { Constants.SC2, Constants.SC3, Constants.SC4, Constants.SC5, Constants.SC6 };
                int c = 0;
                while (playerSCs.isEmpty() && c < 5 && !pickSuccessful) {
                    if (event.getOption(scs[c]) != null) {
                        pickSuccessful = Stats.pickSC(event, game, player, event.getOption(scs[c]));
                    }
                    playerSCs = player.getSCs();
                    c++;
                }
            }
            if (!pickSuccessful) {
                return;
            }
        }
        //ONLY DEAL WITH EXTRA PICKS IF IN FoW
        if (playerSCs.isEmpty()) {
            MessageHelper.sendMessageToEventChannel(event, "No strategy card picked.");
            return;
        }
        StrategyCardPickService.secondHalfOfSCPick(event, player, game, scPicked);
    }

}
