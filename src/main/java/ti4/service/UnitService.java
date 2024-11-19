package ti4.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import ti4.commands.leaders.CommanderUnlockCheck;
import ti4.helpers.AliasHandler;
import ti4.helpers.ButtonHelper;
import ti4.helpers.Constants;
import ti4.helpers.Emojis;
import ti4.helpers.FoWHelper;
import ti4.helpers.Units;
import ti4.image.Mapper;
import ti4.map.Game;
import ti4.map.Player;
import ti4.map.Tile;
import ti4.message.MessageHelper;
import ti4.service.combat.StartCombatService;

@UtilityClass
public class UnitService {

    public static void commonUnitParsing(GenericInteractionCreateEvent event, String color, Tile tile, String unitList, Game game) {
        List<ParsedUnit> parsedUnits = parseUnits(tile, unitList, color, event);
        parsedUnits.forEach(parsedUnit -> {
            doStuff();
        });

        if (game.isFowMode()) {
            boolean pingedAlready = false;
            int countF = 0;
            String[] tileList = game.getListOfTilesPinged();
            while (countF < 10 && !pingedAlready) {
                String tilePingedAlready = tileList[countF];
                if (tilePingedAlready != null) {
                    pingedAlready = tilePingedAlready.equalsIgnoreCase(tile.getPosition());
                    countF++;
                } else {
                    break;
                }
            }
            if (!pingedAlready) {
                String colorMention = Emojis.getColorEmojiWithName(color);
                String message = colorMention + " has modified units in the system. ";
                if (getName().contains("add_units")) {
                    message = message + " Specific units modified include: " + unitList;
                }
                message = message + "Refresh map to see what changed ";
                FoWHelper.pingSystem(game, event, tile.getPosition(), message);
                if (countF < 10) {
                    game.setPingSystemCounter(countF);
                    game.setTileAsPinged(countF, tile.getPosition());
                }
            }
        }

        if (getName().toLowerCase().contains("add_units")) {
            Player player = game.getPlayerFromColorOrFaction(color);
            if (player == null) {
                return;
            }
            ButtonHelper.checkFleetAndCapacity(player, game, tile, event);
            CommanderUnlockCheck.checkPlayer(player, "naalu", "cabal");
        }
    }

    private List<ParsedUnit> parseUnits(Tile tile, String unitList, String color, GenericInteractionCreateEvent event) {
        unitList = unitList.replace(", ", ",");
        StringTokenizer unitListTokenizer = new StringTokenizer(unitList, ",");

        List<ParsedUnit> parsedUnits = new ArrayList<>();
        while (unitListTokenizer.hasMoreTokens()) {
            String unitListToken = unitListTokenizer.nextToken();
            StringTokenizer unitInfoTokenizer = new StringTokenizer(unitListToken, " ");

            int count = 1;
            boolean numberIsSet = false;

            String originalUnit = "";
            String resolvedUnit;
            if (unitInfoTokenizer.hasMoreTokens()) {
                String ifNumber = unitInfoTokenizer.nextToken();
                try {
                    count = Integer.parseInt(ifNumber);
                    numberIsSet = true;
                } catch (Exception e) {
                    originalUnit = ifNumber;
                }
            }
            if (unitInfoTokenizer.hasMoreTokens() && numberIsSet) {
                originalUnit = unitInfoTokenizer.nextToken();
            }
            resolvedUnit = AliasHandler.resolveUnit(originalUnit);

            Units.UnitKey unitKey = Mapper.getUnitKey(resolvedUnit, color);

            String originalPlanetName = "";
            String planetName;
            if (unitInfoTokenizer.hasMoreTokens()) {
                String planetToken = unitInfoTokenizer.nextToken();
                if (unitInfoTokenizer.hasMoreTokens()) {
                    planetToken = planetToken + unitInfoTokenizer.nextToken();
                }
                originalPlanetName = planetToken;
                planetName = AliasHandler.resolvePlanet(planetToken);
            } else {
                planetName = Constants.SPACE;
            }
            planetName = getPlanet(tile, planetName);

            ParsedUnit parsedUnit = new ParsedUnit(unitKey, count, planetName);
            if (!validateParsedUnit(parsedUnit, tile, unitListToken, originalUnit, resolvedUnit, originalPlanetName, planetName, event)) {
                return Collections.emptyList();
            }
            parsedUnits.add(parsedUnit);
        }
        return parsedUnits;
    }

    private static String getPlanet(Tile tile, String planetName) {
        if (tile.isSpaceHolderValid(planetName))
            return planetName;
        return tile.getUnitHolders().keySet().stream()
            .filter(id -> !Constants.SPACE.equals(id))
            .filter(unitHolderID -> unitHolderID.startsWith(planetName))
            .findFirst().orElse(planetName);
    }

    private static boolean validateParsedUnit(ParsedUnit parsedUnit, Tile tile, String unitListToken, String originalUnit, String resolvedUnit,
                                                String originalPlanetName, String planetName, GenericInteractionCreateEvent event) {
        boolean isValidCount = parsedUnit.count > 0;
        boolean isValidUnit = parsedUnit.unitKey != null;
        boolean isValidUnitHolder = Constants.SPACE.equals(parsedUnit.location) || tile.isSpaceHolderValid(parsedUnit.location);

        if (event instanceof SlashCommandInteractionEvent && (!isValidCount || !isValidUnit || !isValidUnitHolder)) {
            String validationMessage = buildValidationMessage(
                unitListToken, isValidCount, parsedUnit.count, isValidUnit, originalUnit, resolvedUnit,
                isValidUnitHolder, originalPlanetName, planetName, tile);
            MessageHelper.sendMessageToChannel(event.getMessageChannel(), validationMessage);
            return false;
        }
        return true;
    }

    private static String buildValidationMessage(String unitListToken, boolean isValidCount, int count, boolean isValidUnit, String originalUnit,
                                                    String resolvedUnit, boolean isValidUnitHolder, String originalPlanetName, String planetName,
                                                    Tile tile) {
        return """
        Could not parse this section of the command: `%s`
        > %s Count = `%d`%s
        > %s Unit = `%s`%s
        > %s Planet = `%s`%s
        """.formatted(
            unitListToken,
            isValidCount ? "✅" : "❌", count, isValidCount ? "" : " -> Count must be a positive integer",
            isValidUnit ? "✅" : "❌", originalUnit,
            isValidUnit ? " -> `%s`".formatted(resolvedUnit)
                : " -> UnitID or Alias not found. Try something like: `inf, mech, dn, car, cru, des, fs, ws, sd, pds`",
            isValidUnitHolder ? "✅" : "❌", originalPlanetName,
            isValidUnitHolder ? " -> `%s`".formatted(planetName)
                : " -> Planets in this system are: `%s`".formatted(
                    String.join(", ", tile.getUnitHolders().keySet())));
    }

    private static void doStuff() {
        int numPlayersOld = 0;
        int numPlayersNew = 0;
        if (event instanceof SlashCommandInteractionEvent) {
            List<Player> playersForCombat = ButtonHelper.getPlayersWithShipsInTheSystem(game, tile);
            if (!planetName.equalsIgnoreCase("space") && !game.isFowMode()) {
                playersForCombat = ButtonHelper.getPlayersWithUnitsOnAPlanet(game, tile, planetName);
            }
            numPlayersOld = playersForCombat.size();
        }

        unitAction(event, tile, count, planetName, unitID, color, game);
        
        if (event instanceof SlashCommandInteractionEvent && !game.isFowMode()) {
            List<Player> playersForCombat = ButtonHelper.getPlayersWithShipsInTheSystem(game, tile);
            if (!planetName.equalsIgnoreCase("space")) {
                playersForCombat = ButtonHelper.getPlayersWithUnitsOnAPlanet(game, tile, planetName);
            }
            numPlayersNew = playersForCombat.size();
        }
        addPlanetToPlayArea(event, tile, planetName, game);
        if (numPlayersNew > numPlayersOld && numPlayersOld != 0) {
            List<Player> playersForCombat = ButtonHelper.getPlayersWithShipsInTheSystem(game, tile);
            String combatType = "space";
            if (!planetName.equalsIgnoreCase("space")) {
                combatType = "ground";
                playersForCombat = ButtonHelper.getPlayersWithUnitsOnAPlanet(game, tile, planetName);
            }

            // Try to get players in order of [activePlayer, otherPlayer, ... (discarded players)]
            Player player1 = game.getActivePlayer();
            if (player1 == null)
                player1 = playersForCombat.getFirst();
            playersForCombat.remove(player1);
            Player player2 = player1;
            for (Player p2 : playersForCombat) {
                if (p2 != player1 && !player1.getAllianceMembers().contains(p2.getFaction())) {
                    player2 = p2;
                    break;
                }
            }
            if (player1 != player2 && !tile.getPosition().equalsIgnoreCase("nombox") && !player1.getAllianceMembers().contains(player2.getFaction())) {
                if ("ground".equals(combatType)) {
                    StartCombatService.startGroundCombat(player1, player2, game, event, tile.getUnitHolderFromPlanet(planetName), tile);
                } else {
                    StartCombatService.startSpaceCombat(game, player1, player2, tile, event);
                }
            }
        }
    }

    private record ParsedUnit(Units.UnitKey unitKey, int count, String location) {}
}
