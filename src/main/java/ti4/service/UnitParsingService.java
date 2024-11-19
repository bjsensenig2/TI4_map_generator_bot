package ti4.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import ti4.helpers.AliasHandler;
import ti4.helpers.Constants;
import ti4.helpers.Units;
import ti4.image.Mapper;
import ti4.map.Game;
import ti4.map.Tile;
import ti4.message.MessageHelper;

@UtilityClass
public class UnitParsingService {

    public static List<ParsedUnit> parseUnits(GenericInteractionCreateEvent event, String color, Tile tile, String unitList, Game game) {
        unitList = unitList.replace(", ", ",").replace("-", "").replace("'", "").toLowerCase();
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
            if (!validateParsedUnit(parsedUnit, tile, unitListToken, originalUnit, resolvedUnit, originalPlanetName, planetName, color, event)) {
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
                                                String originalPlanetName, String planetName, String color, GenericInteractionCreateEvent event) {
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

    public record ParsedUnit(Units.UnitKey unitKey, int count, String location) {}
}
