package me.Wundero.Ray.utils;
/*
 The MIT License (MIT)

 Copyright (c) 2016 Wundero

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.apache.commons.lang3.Validate;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.scoreboard.Score;
import org.spongepowered.api.statistic.achievement.Achievement;
import org.spongepowered.api.text.LiteralText;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.TextTemplate.Arg;
import org.spongepowered.api.text.action.ClickAction;
import org.spongepowered.api.text.action.HoverAction;
import org.spongepowered.api.text.action.ShiftClickAction;
import org.spongepowered.api.text.action.TextAction;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextFormat;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.selector.Selector;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.text.translation.Translatable;
import org.spongepowered.api.text.translation.Translation;
import org.spongepowered.api.text.translation.locale.Locales;

import com.flowpowered.math.GenericMath;

import me.Wundero.Ray.Ray;
import me.Wundero.Ray.config.InternalClickAction;
import me.Wundero.Ray.config.InternalHoverAction;
import me.Wundero.Ray.variables.ParsableData;

/**
 * A class containing methods that help parse Text objects.
 */
public class TextUtils {

	/*
	 * Font size rendering - Credit to simon816
	 */
	private static final String ASCII_PNG_CHARS = "ÀÁÂÈÊËÍÓÔÕÚßãõğİ"
			+ "ıŒœŞşŴŵžȇ\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + " !\"#$%&\'()*+,-./" + "0123456789:;<=>?"
			+ "@ABCDEFGHIJKLMNO" + "PQRSTUVWXYZ[\\]^_" + "`abcdefghijklmno" + "pqrstuvwxyz{|}~\u0000"
			+ "ÇüéâäàåçêëèïîìÄÅ" + "ÉæÆôöòûùÿÖÜø£Ø×ƒ" + "áíóúñÑªº¿®¬½¼¡«»" + "░▒▓│┤╡╢╖╕╣║╗╝╜╛┐" + "└┴┬├─┼╞╟╚╔╩╦╠═╬╧"
			+ "╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀" + "αβΓπΣσμτΦΘΩδ∞∅∈∩" + "≡±≥≤⌠⌡÷≈°∙·√ⁿ²■\u0000";

	private static final int[] ASCII_PNG_CHAR_WIDTHS = new int[ASCII_PNG_CHARS.length()];
	private static final byte[] UNICODE_CHAR_WIDTHS = new byte[65536];

	static {
		try {
			computeCharWidths();
			InputStream gStream = TextUtils.class.getResourceAsStream("glyph_sizes.bin");
			gStream.read(UNICODE_CHAR_WIDTHS);
			gStream.close();
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static void computeCharWidths() throws IOException {
		InputStream iStream = TextUtils.class.getResourceAsStream("ascii.png");
		BufferedImage img = ImageIO.read(iStream);
		iStream.close();
		int width = img.getWidth();
		int height = img.getHeight();
		int[] imgData = new int[width * height];
		img.getRGB(0, 0, width, height, imgData, 0, width);
		int charH = height / 16;
		int charW = width / 16;
		float lvt_9_1_ = 8.0F / (float) charW;
		for (int idx = 0; idx < 256; ++idx) {
			if (idx == 32) {
				ASCII_PNG_CHAR_WIDTHS[idx] = 4;
				continue;
			}
			int col = idx % 16;
			int row = idx / 16;
			int offX;
			for (offX = charW - 1; offX >= 0; --offX) {
				int imgX = col * charW + offX;
				boolean hasValue = true;
				for (int offY = 0; offY < charH && hasValue; ++offY) {
					int imgY = (row * charW + offY) * width;
					if ((imgData[imgX + imgY] >> 24 & 255) != 0) {
						hasValue = false;
					}
				}
				if (!hasValue) {
					break;
				}
			}
			++offX;
			ASCII_PNG_CHAR_WIDTHS[idx] = (int) (0.5D + (double) ((float) offX * lvt_9_1_)) + 1;
		}
	}

	/**
	 * Get the width of a character.
	 */
	public static double getWidth(int codePoint, boolean isBold, boolean forceUnicode) {
		if (codePoint == '\n') {
			return 0;
		}
		if (codePoint == 32) {
			return 4 + (isBold ? 1 : 0);
		}
		int nonUnicodeIdx = forceUnicode ? -1 : ASCII_PNG_CHARS.indexOf(codePoint);
		double width;
		if (codePoint > 0 && nonUnicodeIdx != -1) {
			width = ASCII_PNG_CHAR_WIDTHS[nonUnicodeIdx];
		} else {
			int squashedVal = UNICODE_CHAR_WIDTHS[codePoint] & 255;
			if (squashedVal == 0) {
				return 0;
			}
			int upper = squashedVal >>> 4;
			int lower = squashedVal & 15;
			width = ((lower + 1) - upper) / 2 + 1;
		}
		if (isBold && width > 0) {
			width += 1;
		}
		return width;
	}

	/**
	 * Get the widths of all characters in the string.
	 */
	public static int getStringWidth(String text, boolean isBold, boolean forceUnicode) {
		double width = 0;
		for (int i = 0; i < text.length(); ++i) {
			width += getWidth(text.codePointAt(i), isBold, forceUnicode);
		}
		return (int) Math.ceil(width);
	}

	/**
	 * Get the width of a character.
	 */
	public static int getWidth(char c, boolean isBold, boolean forceUnicode) {
		return (int) Math.ceil(getWidth((int) c, isBold, forceUnicode));
	}

	/**
	 * Get the width of a text object's content.
	 */
	public static int getWidth(Text text, boolean forceUnicode) {
		return (int) Math.ceil(getWidth0(text, false, forceUnicode));
	}

	/**
	 * Get the width of a text object's content.
	 */
	public static int getWidth(Text text) {
		return getWidth(text, false);
	}

	private static double getWidth0(Text text, boolean parentIsbold, boolean forceUnicode) {
		double width = 0;
		boolean thisIsBold = text.getStyle().isBold().orElse(parentIsbold);
		if (text instanceof LiteralText) {
			String content = ((LiteralText) text).getContent();
			width += getStringWidth(content, thisIsBold, forceUnicode);
			for (Text child : text.getChildren()) {
				width += getWidth0(child, thisIsBold, forceUnicode);
			}
		} else {
			width += getStringWidth(text.toPlain(), thisIsBold, forceUnicode);
		}
		return width;
	}

	/*
	 * End of font rendering
	 */
	private static final int LINE_WIDTH = 320;

	/**
	 * Center text between padding, with a space
	 */
	public static Text center(Text text, Text padding) {
		int inputLength = getWidth(text);
		// Minecraft breaks lines when the next character would be > then
		// LINE_WIDTH, this seems most graceful way to fail
		if (inputLength >= LINE_WIDTH) {
			return text;
		}
		Text styledSpace = withStyle(LiteralText.of(" "), text);
		Text textWithSpaces = addSpaces(styledSpace, text);

		// Minecraft breaks lines when the next character would be > then
		// LINE_WIDTH
		boolean addSpaces = getWidth(textWithSpaces) <= LINE_WIDTH;

		int paddingLength = getWidth(padding);
		final Text.Builder output = Text.builder();

		// Using 0 width unicode symbols as padding throws us into an unending
		// loop, replace them with the default padding
		if (paddingLength < 1) {
			Text defaultPadding = Text.of("=");
			padding = defaultPadding;
			paddingLength = getWidth(withStyle(defaultPadding, text));
		}

		// if we only need padding
		if (inputLength == 0) {
			addPadding(padding, output, GenericMath.floor((double) LINE_WIDTH / paddingLength));
		} else {
			if (addSpaces) {
				text = textWithSpaces;
				inputLength = getWidth(textWithSpaces);
			}

			int paddingNecessary = LINE_WIDTH - inputLength;

			int paddingCount = GenericMath.floor(paddingNecessary / paddingLength);
			// pick a halfway point
			int beforePadding = GenericMath.floor(paddingCount / 2.0);
			// Do not use ceil, this prevents floating point errors.
			int afterPadding = paddingCount - beforePadding;

			addPadding(padding, output, beforePadding);
			output.append(text);
			addPadding(padding, output, afterPadding);
		}

		return finalizeBuilder(text, output);
	}

	private static Text withStyle(Text text, Text styled) {
		return text.toBuilder().color(styled.getColor()).style(styled.getStyle()).build();
	}

	private static Text finalizeBuilder(Text text, Text.Builder build) {
		build.color(text.getColor()).style(text.getStyle());
		return build.build();
	}

	private static Text addSpaces(Text spaces, Text text) {
		Text.Builder build = Text.builder().color(text.getColor()).style(text.getStyle());
		build.append(spaces);
		build.append(text);
		build.append(spaces);
		return build.build();
	}

	private static void addPadding(Text padding, Text.Builder build, int count) {
		if (count > 0) {
			build.append(Collections.nCopies(count, padding));
		}
	}

	/**
	 * Get the number of lines for a text object. If it contains newlines,
	 * account for those as well.
	 */
	public static int getLines(Text text) {
		if (contains(text, "\n")) {
			List<Text> spl = newlines(text);
			int total = 0;
			for (Text s : spl) {
				total += getLines(s);
			}
			return total;
		}
		int val = (int) Math.ceil((double) getWidth(text, false) / LINE_WIDTH);
		return val;
	}

	private static List<Text> splitLines(Text original, int maxWidth, Locale locale, boolean forceUnicode) {
		List<Text> output = Utils.al();
		TextSplitter.splitLines(original, output, maxWidth, locale, forceUnicode);
		return output;
	}

	/**
	 * Split a text after every line. Credit to simon816 for proper function of
	 * this algorithm.
	 */
	public static List<Text> splitIntoLines(Text t) {
		if (getLines(t) > 1) {
			return splitLines(t, LINE_WIDTH, Locales.DEFAULT, false);
		} else {
			return Utils.al(t);
		}
	}

	/**
	 * Get total number of lines for a list of text objects.
	 */
	public static int getLines(List<Text> texts) {
		int i = 0;
		for (Text t : texts) {
			i += getLines(t);
		}
		return i;
	}

	/**
	 * Pattern to match against color codes.
	 */
	public static final Pattern COLOR_PATTERN = Utils.compile("\\&[a-f0-9]", Pattern.CASE_INSENSITIVE);

	/**
	 * Return an empty text.
	 */
	public static Text EMPTY() {
		return Text.EMPTY;
	}

	/**
	 * Return a blank text.
	 */
	public static Text BLANK() {
		return Text.of("");
	}

	/**
	 * Return a text with a space character.
	 */
	public static Text SPACE() {
		return Text.of(" ");
	}

	/**
	 * Wipe the color from a text object.
	 */
	public static Text strip(Text t) {
		Text.Builder b = t.toBuilder();
		List<Text> ch = b.getChildren();
		b.removeAll();
		b.format(TextFormat.NONE);
		for (Text f : ch) {
			b.append(strip(f));
		}
		return b.build();
	}

	/**
	 * Create a new text object.
	 */
	public static Text of() {
		return Text.of();
	}

	/**
	 * Create a new text object.
	 */
	public static Text of(char character) {
		return Text.of(character);
	}

	/**
	 * Create a new text object.
	 */
	public static Text of(Translation t, Object... args) {
		return Text.of("").concat(Text.of(t, args));
	}

	/**
	 * Create a new text object.
	 */
	public static Text of(Translatable t, Object... args) {
		return Text.of("").concat(Text.of(t, args));
	}

	/**
	 * Create a new text object.
	 */
	public static Text of(Selector sel) {
		return Text.of(sel);
	}

	/**
	 * Create a new text object.
	 */
	public static Text of(Score score) {
		return Text.of(score);
	}

	/**
	 * Create a new text object. Unlike normal creation, TextActions will not be
	 * applied to children.
	 */
	public static Text of(Object... contents) {
		return Text.of("").concat(Text.of(contents));
	}

	/**
	 * Create a new text object.
	 */
	public static Text of(String s) {
		return Text.of(s);
	}

	/**
	 * If the player has permission, translate the string into a colored text,
	 * else a plain text with formatting codes. ALso parses URLS if they have
	 * permission.
	 */
	public static Text transIf(String s, User u) {
		LiteralText text = null;
		if (u == null || u.hasPermission("ray.color")) {
			text = (LiteralText) TextSerializers.FORMATTING_CODE.deserialize(s);
		} else {
			text = Text.of(s);
		}
		if (u == null || u.hasPermission("ray.url")) {
			return urls(text);
		} else {
			return text;
		}
	}

	/**
	 * Split a text and it's children into multiple text objects.
	 */
	public static List<Text> flatten(Text t) {
		if (t.getChildren().isEmpty()) {
			return Utils.al(t);
		}
		List<Text> children = Utils.al(t.getChildren(), true);
		children.add(0, t.toBuilder().removeAll().build());
		return children;
	}

	/**
	 * Recursively split a text and it's children into multiple text objects.
	 * Will recurse over children as well, until no text in the list contains
	 * children.
	 */
	public static List<Text> flatten(final Text text, boolean recursive) {
		if (text.getChildren().isEmpty()) {
			return Utils.al(text);
		}
		if (!recursive) {
			return flatten(text);
		}
		Text t = TextUtils.condense(text);
		List<Text> f = Utils.al(t.getChildren(), true);
		List<Text> out = Utils.al(t.toBuilder().removeAll().build());
		f.stream().forEach(texte -> out.addAll(flatten(mergeOnto(t, texte), recursive)));
		return out;
	}

	/**
	 * Merge text formats and on clicks from one text onto another without
	 * overriding
	 */
	public static Text mergeOnto(Text from, Text onto) {
		TextFormat one = from.getFormat();
		TextFormat two = onto.getFormat();
		TextFormat outformat = one.merge(two);
		Text.Builder out = onto.toBuilder().format(outformat);
		if (!onto.getClickAction().isPresent()) {
			from.getClickAction().ifPresent(c -> out.onClick(c));
		}
		if (!onto.getShiftClickAction().isPresent()) {
			from.getShiftClickAction().ifPresent(c -> out.onShiftClick(c));
		}
		if (!onto.getHoverAction().isPresent()) {
			from.getHoverAction().ifPresent(c -> out.onHover(c));
		}
		return out.build();
	}

	/**
	 * Apply all text actions, in order, to the text.
	 */
	public static Text.Builder applyAll(Text.Builder builder, TextAction<?>... actions) {
		for (TextAction<?> act : actions) {
			act.applyTo(builder);
		}
		return builder;
	}

	/**
	 * Darken or replace characters from the original text based off of the
	 * percentages provided.
	 */
	public static Text obfuscate(Text original, Double percentObfuscation, Double percentDiscoloration) {
		if (original == null) {
			return Text.of();
		}
		List<Text> children = original.getChildren();
		original = original.toBuilder().removeAll().build();
		Text.Builder out = Text.builder().color(original.getColor());
		char[] chars = getContent(original).toCharArray();
		Integer obC = percentDiscoloration.intValue();
		Integer obS = percentObfuscation.intValue();
		List<Text> cs = Utils.al();
		Random rng = new Random();
		TextColor co = original.getColor();
		for (Character c : chars) {
			if (rng.nextInt(100) < obS) {
				cs.add(Text.of(co, " "));
			} else {
				cs.add(Text.of(co, c));
			}
		}
		List<Text> toBuild = Utils.al();
		for (Text t : cs) {
			if (rng.nextInt(100) < obC) {
				toBuild.add(t.toBuilder().color(TextColors.DARK_GRAY).build());
			} else {
				toBuild.add(t);
			}
		}
		out.append(toBuild);
		for (Text t : children) {
			out.append(obfuscate(t, percentObfuscation, percentDiscoloration));
		}
		return out.build();
	}

	/**
	 * Find all variables in a string and parse it into a text template.
	 */
	public static TextTemplate parse(final String i, boolean allowColors) {
		if (i == null) {
			return null;
		}
		String in = i;
		if (!allowColors) {
			in = strip(in);
		}
		in = in.replace("%n", "\n");
		if (!Utils.VAR_PATTERN.matcher(in).find()) {
			return TextTemplate.of(TextSerializers.FORMATTING_CODE.deserialize(in));
		}
		String[] textParts = in.split(Utils.VAR_PATTERN.pattern());
		if (textParts.length == 0) {
			return TextTemplate.of(TextTemplate.arg(in));
		}
		Matcher matcher = Utils.VAR_PATTERN.matcher(in);
		TextTemplate out = TextTemplate.of(TextSerializers.FORMATTING_CODE.deserialize(textParts[0]));
		int x = 1;
		while (matcher.reset(in).find()) {
			String mg = matcher.group().substring(1);
			mg = mg.substring(0, mg.length() - 1);
			out = out.concat(TextTemplate.of(TextTemplate.arg(mg)));
			if (x < textParts.length) {
				out = out.concat(TextTemplate.of(TextSerializers.FORMATTING_CODE.deserialize(textParts[x])));
			}
			in = matcher.replaceFirst("");
			x++;
		}
		return out;
	}

	/**
	 * Create an internalclickaction with a url template.
	 */
	public static InternalClickAction<?> urlTemplate(TextTemplate t) {
		return new InternalClickAction.UrlTemplate(t);
	}

	/**
	 * Create an internalclickaction with a suggest template.
	 */
	public static InternalClickAction<?> suggestTemplate(TextTemplate t) {
		return new InternalClickAction.SuggestTemplate(t);
	}

	/**
	 * Create an internalclickaction with a run template.
	 */
	public static InternalClickAction<?> runTemplate(TextTemplate t) {
		return new InternalClickAction.RunTemplate(t);
	}

	/**
	 * Create an internalclickaction with a callback.
	 */
	public static InternalClickAction<?> executeCallback(Consumer<CommandSource> c) {
		return new InternalClickAction.ExecuteCallback(c);
	}

	/**
	 * Create an internalclickaction with a page change.
	 */
	public static InternalClickAction<?> changePage(int i) {
		return new InternalClickAction.ChangePage(i);
	}

	/**
	 * Create an internalclickaction with a url.
	 */
	public static InternalClickAction<?> openUrl(URL u) {
		return new InternalClickAction.OpenUrl(u);
	}

	/**
	 * Create an internalclickaction with a suggest command.
	 */
	public static InternalClickAction<?> suggestCommand(String s) {
		return new InternalClickAction.SuggestCommand(s);
	}

	/**
	 * Create an internalclickaction with a run command.
	 */
	public static InternalClickAction<?> runCommand(String s) {
		return new InternalClickAction.RunCommand(s);
	}

	/**
	 * Create an internalhoveraction with an entity
	 */
	public static InternalHoverAction.ShowEntity showEntity(InternalHoverAction.ShowEntity.Ref entity) {
		return new InternalHoverAction.ShowEntity(entity);
	}

	/**
	 * Create an internalhoveraction with an entity
	 */
	public static InternalHoverAction.ShowEntity showEntity(UUID uuid, String name, @Nullable EntityType type) {
		return showEntity(new InternalHoverAction.ShowEntity.Ref(uuid, name, type));
	}

	/**
	 * Create an internalhoveraction with an entity
	 */
	public static InternalHoverAction.ShowEntity showEntity(UUID uuid, String name) {
		return showEntity(new InternalHoverAction.ShowEntity.Ref(uuid, name));
	}

	/**
	 * Create an internalhoveraction with an entity
	 */
	public static InternalHoverAction.ShowEntity showEntity(Entity entity, String name) {
		return showEntity(new InternalHoverAction.ShowEntity.Ref(entity, name));
	}

	/**
	 * Create an internalhoveraction with an item
	 */
	public static InternalHoverAction<?> showItem(ItemStackSnapshot i) {
		return new InternalHoverAction.ShowItem(i);
	}

	/**
	 * Create an internalhoveraction with an achievement
	 */
	public static InternalHoverAction<?> showAchievement(Achievement a) {
		return new InternalHoverAction.ShowAchievement(a);
	}

	/**
	 * Create an internalhoveraction with a text.
	 */
	public static InternalHoverAction<?> showText(Text t) {
		return new InternalHoverAction.ShowText(t);
	}

	/**
	 * Create an internalhoveraction with a template.
	 */
	public static InternalHoverAction<?> showTemplate(TextTemplate t) {
		return new InternalHoverAction.ShowTemplate(t);
	}

	/**
	 * Remove colors from a string. text boolean has no value check; just
	 * ensures method signatures are proper.
	 * 
	 * @return a text object which has been stripped of color.
	 */
	public static Text strip(String s, boolean text) {
		return Text.of(strip(s));
	}

	/**
	 * Return a String which has been stripped of formatting code colors.
	 */
	public static String strip(String s) {
		if (s == null) {
			return "";
		}
		if (s.contains("\\u00a7")) {
			s = s.replace("\\u00a7", "&");
		}
		return TextSerializers.FORMATTING_CODE.stripCodes(s);
	}

	/**
	 * Prepend two onto one.
	 */
	public static Text prepend(Text one, Text two) {
		return two.concat(one);
	}

	/**
	 * Append two onto one.
	 */
	public static Text append(Text one, Text two) {
		return one.concat(two);
	}

	/**
	 * Insert two into one.
	 */
	public static Text insert(Text one, Text two, int index) {
		if (index == 0) {
			return two.concat(one);
		}
		if (index == length(one)) {
			return one.concat(two);
		}
		return substring(one, 0, index).concat(two).concat(substring(one, index));
	}

	/**
	 * Remove all occurances of two in one.
	 */
	public static Text remove(Text one, Text two) {
		Text o = one;
		int i = 0;
		int l = length(two);
		while ((i = indexOf(o, two)) >= 0) {
			o = substring(o, 0, i).concat(substring(o, i + l));
		}
		return o;
	}

	/**
	 * Overlay one part of a text with another text.
	 */
	public static Text overlay(Text original, Text overlay, int start, int finish) {
		Validate.isTrue(start > 0);
		Validate.isTrue(finish > start);
		Text.Builder b = Text.builder("");
		if (start > 0) {
			b.append(substring(original, 0, start));
		}
		b.append(overlay);
		if (finish < length(original)) {
			b.append(substring(original, finish));
		}
		return b.build();
	}

	/**
	 * Parse colors from a String into a text object.
	 */
	public static Text trans(String s) {
		if (s == null) {
			return Text.of();
		}
		if (s.contains("\\u00a7")) {
			s = s.replace("\\u00a7", "&");
		}
		if (!s.contains("&")) {
			return Text.of(s);
		}
		return TextSerializers.FORMATTING_CODE.deserialize(s);
	}

	/**
	 * Replace regular expression in a LiteralText object's content.
	 * 
	 * Note: this method does NOT check between object nodes. So if there is a
	 * match between the original and one of it's children, it is not parsed.
	 * 
	 * @param original
	 *            The original text.
	 * @param matcher
	 *            The pattern to search for in the content.
	 * @param replacer
	 *            The function to call when a match is found in order to supply
	 *            a textual replacement.
	 * @param useClickHover
	 *            Whether or not to parse click and hover actions as well.
	 */
	public static LiteralText replaceRegex(LiteralText original, Pattern matcher,
			Function<LiteralText, Optional<LiteralText>> replacer, boolean useClickHover) {
		if (original == null) {
			return null;
		}
		List<Text> children = original.getChildren();
		LiteralText.Builder builder = original.toBuilder();
		TextFormat f = builder.getFormat();
		Optional<ClickAction<?>> cl = builder.getClickAction();
		Optional<HoverAction<?>> ho = builder.getHoverAction();
		Optional<ShiftClickAction<?>> sc = builder.getShiftClickAction();
		builder.removeAll();
		String content = builder.getContent();
		if (matcher.matcher(content).find()) {
			Matcher m = matcher.matcher(content);
			if (m.matches()) {
				builder = replacer.apply(builder.build()).orElse(LiteralText.of("")).toBuilder();
			} else {
				String[] parts = content.split(matcher.pattern());
				if (parts.length == 0) {
					String c = content;
					builder = null;
					while ((m = m.reset(c)).find()) {
						String g = m.group();
						LiteralText t = replacer.apply(
								(LiteralText) TextUtils.apply(LiteralText.builder(g).format(f), cl, ho, sc).build())
								.orElse(LiteralText.of(""));
						if (builder == null) {
							builder = t.toBuilder();
						} else {
							builder.append(t);
						}
						c = m.replaceFirst("");
					}
				} else {
					if (content.startsWith(parts[0])) {
						List<String> alt = Utils.alternate(parts, allmatches(content, matcher));
						LiteralText.Builder ob = builder;
						builder = (LiteralText.Builder) LiteralText.builder();
						List<String> pz = Utils.al(parts, true);
						for (String s : alt) {
							if (pz.contains(s)) {
								LiteralText t = replaceRegex(bf(s, ob).build(), matcher, replacer, useClickHover);
								builder.append(t);
							} else {
								Optional<LiteralText> t = replacer.apply((LiteralText) TextUtils
										.apply(LiteralText.builder(s).format(f), cl, ho, sc).build());
								builder.append(t.orElse(LiteralText.of("")));
							}
						}
					} else {
						List<String> alt = Utils.alternate(allmatches(content, matcher), parts);
						LiteralText.Builder ob = builder;
						builder = (LiteralText.Builder) LiteralText.builder();
						List<String> pz = Utils.al(parts, true);
						for (String s : alt) {
							if (pz.contains(s)) {
								builder.append(replaceRegex(bf(s, ob).build(), matcher, replacer, useClickHover));
							} else {
								Optional<LiteralText> t = replacer.apply((LiteralText) TextUtils
										.apply(LiteralText.builder(s).format(f), cl, ho, sc).build());
								builder.append(t.orElse(LiteralText.of("")));
							}
						}
					}
				}
			}
		} else {
			if (builder.getClickAction().isPresent()) {
				boolean r = builder.getClickAction().get() instanceof ClickAction.RunCommand;
				String click = replaceRegexAction(builder.getClickAction().get(), (s) -> {
					if (!matcher.matcher(s).find()) {
						return s;
					}
					String out = matcher.matcher(s).replaceAll("%s");
					String st = s;
					Matcher m = matcher.matcher(s);
					List<Object> b = Utils.al();
					while ((m = m.reset(st)).find()) {
						String g = m.group();
						b.add(replacer.apply(
								(LiteralText) TextUtils.apply(LiteralText.builder(g).format(f), cl, ho, sc).build())
								.orElse(LiteralText.builder("").build()).toPlain());
						st = m.replaceFirst("");
					}
					out = format(out, b);
					return out;
				}, "");
				if (!click.isEmpty()) {
					if (r) {
						builder.onClick(TextActions.runCommand(click));
					} else {
						builder.onClick(TextActions.suggestCommand(click));
					}
				}
			}
			if (builder.getHoverAction().isPresent()) {
				Text hover = replaceRegexAction(builder.getHoverAction().get(), (s) -> {
					String a = TextSerializers.FORMATTING_CODE.serialize(s);
					if (!matcher.matcher(a).find()) {
						return s;
					}
					String out = matcher.matcher(a).replaceAll("%s");
					String st = a;
					Matcher m = matcher.matcher(a);
					List<Object> b = Utils.al();
					while ((m = m.reset(st)).find()) {
						String g = m.group();
						b.add(TextSerializers.FORMATTING_CODE.serialize(replacer.apply(
								(LiteralText) TextUtils.apply(LiteralText.builder(g).format(f), cl, ho, sc).build())
								.orElse(LiteralText.builder("").build())));
						st = m.replaceFirst("");
					}
					out = format(out, b);
					return TextSerializers.FORMATTING_CODE.deserialize(out);
				}, Text.of());
				if (!hover.isEmpty()) {
					builder.onHover(TextActions.showText(hover));
				}
			}
		}
		builder.append(children.stream().filter(t -> lit(t))
				.map(child -> replaceRegex((LiteralText) child, matcher, replacer, useClickHover))
				.collect(RayCollectors.rayList()));
		return builder.build();
	}

	/**
	 * Apply the actions from one builder onto another.
	 */
	public static Text.Builder apply(Text.Builder onto, Text.Builder from) {
		return apply(onto, from.getClickAction(), from.getHoverAction(), from.getShiftClickAction());
	}

	/**
	 * Apply the actions provided onto a builder.
	 */
	public static Text.Builder apply(Text.Builder b, Optional<ClickAction<?>> c, Optional<HoverAction<?>> h,
			Optional<ShiftClickAction<?>> s) {
		c.ifPresent(x -> b.onClick(x));
		h.ifPresent(x -> b.onHover(x));
		s.ifPresent(x -> b.onShiftClick(x));
		return b;
	}

	/**
	 * Split the text object at a character reference. If skip, char will be
	 * skipped.
	 */
	public static List<Text> split(Text t, char c, boolean skip) {
		return split(t, String.valueOf(c), skip);
	}

	/**
	 * Split the text object at a string reference. If skip, string will be
	 * skipped.
	 */
	public static List<Text> split(Text t, String s, boolean skip) {
		return split(t, Utils.compile(s, Pattern.LITERAL), skip);
	}

	/**
	 * Split the text at a regular expression. If skip, pattern will be skipped.
	 */
	public static List<Text> split(Text t, Pattern p, boolean skip) {
		if (!lit(t)) {
			return Utils.al(t);
		}
		List<Text> out = Utils.al();
		List<Text> children = t.getChildren();
		LiteralText.Builder text = ((LiteralText) t).toBuilder();
		String content = text.getContent();
		if (!p.matcher(content).find()) {
			return Utils.al(t);
		}
		if (p.matcher(content).matches()) {
			return skip ? Utils.al() : Utils.al(t);
		}
		Matcher m = p.matcher(content);
		while ((m = m.reset(content)).find()) {
			int s = m.start();
			int e = m.end();
			Text.Builder b = Text.builder(content.substring(0, s)).format(text.getFormat());
			b = apply(b, text);
			out.add(b.build());
			if (!skip) {
				b = Text.builder(m.group()).format(text.getFormat());
				b = apply(b, text);
				out.add(b.build());
			}
			content = content.substring(0, e);
		}
		if (!content.isEmpty()) {
			Text.Builder b = Text.builder(content).format(text.getFormat());
			b = apply(b, text);
			out.add(b.build());
		}
		Text.Builder tx = out.get(out.size() - 1).toBuilder();
		out.remove(out.size() - 1);
		for (Text child : children) {
			List<Text> lt = split(child, p, skip);
			if (lt.isEmpty()) {
				out.add(tx.build());
				tx = null;
			} else if (lt.size() == 1) {
				tx = tx == null ? lt.get(0).toBuilder() : tx.append(lt.get(0));
			} else {
				out.add(tx == null ? lt.get(0) : tx.append(lt.get(0)).build());
				for (int i = 1; i < lt.size() - 1; i++) {
					out.add(lt.get(i));
				}
				tx = tx == null ? lt.get(lt.size() - 1).toBuilder() : lt.get(lt.size() - 1).toBuilder();
			}
		}
		if (tx != null) {
			out.add(tx.build());
		}
		return out;
	}

	/**
	 * Change the content of the text.
	 */
	public static Text forEachString(final Text t, final Function<String, String> replacer) {
		String content = t.toPlainSingle();
		String nc = replacer.apply(content);
		List<Text> children = t.getChildren();
		Text.Builder out = Text.builder(nc).format(t.getFormat());
		out = TextUtils.apply(out, t.toBuilder());
		List<Text> newchildren = children.stream().map(child -> forEachString(child, replacer))
				.collect(RayCollectors.rayList());
		return out.append(newchildren).build();
	}

	/**
	 * Change each part of the text.
	 */
	public static Text forEachText(final Text t, final Function<Text, Text> replacer) {
		Text t2 = t.toBuilder().removeAll().build();
		t2 = replacer.apply(t2);
		return t2.toBuilder()
				.append(t.getChildren().stream().map(c -> forEachText(c, replacer)).collect(RayCollectors.rayList()))
				.build();
	}

	/**
	 * Split the text at all newline characters.
	 */
	public static List<Text> newlines(Text t) {
		return split(t, '\n', true);
	}

	/**
	 * Split the text after a certain number of content characters has been
	 * passed. Good for preventing packet overflow of texts with more than
	 * 32,767 characters. [Though it should be noted that you should split at a
	 * number less than or equal to 15,000, to both save on performance and
	 * account for extra characters included in the object]
	 */
	public static Text[] splitAfterCharCount(Text t, int charCount, boolean repeat, boolean arr) {
		return splitAfterCharCount(t, charCount, repeat).stream().toArray(i -> new Text[i]);
	}

	/**
	 * Split the text after a certain number of content characters has been
	 * passed. Good for preventing packet overflow of texts with more than
	 * 32,767 characters. [Though it should be noted that you should split at a
	 * number less than or equal to 15,000, to both save on performance and
	 * account for extra characters included in the object]
	 */
	public static List<Text> splitAfterCharCount(Text t, int charCount, boolean repeat) {
		if (!lit(t)) {
			return Utils.al(t);
		}
		List<Text> out = Utils.al();
		int times = length(t) % charCount;
		if (!repeat) {
			times = 1;
		}
		for (int i = 0; i <= times; i++) {
			int start = i * charCount;
			int fin = (i + 1) * charCount;
			out.add(substring(t, start, fin));
		}
		return out;
	}

	/**
	 * Substring a text starting at an index. Follows String.substring(a).
	 */
	public static Text substring(Text t, int start) {
		return substring(t, start, length(t));
	}

	/**
	 * Substring a text from one index to another. Follows the same rules as
	 * String.substring(a,b);
	 */
	public static Text substring(Text t, int start, int finish) {
		if (!lit(t)) {
			return t;
		}
		if (start < 0) {
			throw new IllegalArgumentException("Cannot start below 0!");
		}
		if (finish < start) {
			throw new IllegalArgumentException("End must be after start!");
		}
		if (finish == start) {
			return Text.of();
		}
		String c = getContent(t);
		if (finish > c.length()) {
			throw new IllegalArgumentException("End must be within text!");
		}
		Text t2 = t;
		if (start > 0) {
			List<Map.Entry<String, Boolean>> lens = getUpTo(t2, start);
			int len = 0;
			for (Map.Entry<String, Boolean> e : lens) {
				len += getStringWidth(e.getKey(), e.getValue(), false);
			}
			List<Text> texts = Utils.al();
			TextSplitter.splitLines(t2, texts, len, Locales.DEFAULT, false);
			t2 = Text.join(texts.stream().skip(1).collect(RayCollectors.rayList()));
		}
		List<Map.Entry<String, Boolean>> lens = getUpTo(t2, finish);
		int len = 0;
		for (Map.Entry<String, Boolean> e : lens) {
			len += getStringWidth(e.getKey(), e.getValue(), false);
		}
		List<Text> texts = Utils.al();
		TextSplitter.splitLines(t2, texts, len, Locales.DEFAULT, false);
		return texts.get(0);
	}

	private static int getl(List<Map.Entry<String, Boolean>> l) {
		int i = 0;
		for (Map.Entry<String, Boolean> e : l) {
			i += e.getKey().length();
		}
		return i;
	}

	private static List<Map.Entry<String, Boolean>> getUpTo(Text t, int ind) {
		List<Text> c = Utils.al(t.getChildren(), true);
		Text t2 = t.toBuilder().removeAll().build();
		String s = getContent(t2);
		if (s.length() >= ind) {
			String s2 = s.substring(0, ind);
			Map<String, Boolean> ent = Utils.hm();
			ent.put(s2, t2.getStyle().isBold().orElse(false));
			return Utils.al(ent.entrySet(), true);
		} else {
			Map<String, Boolean> ent = Utils.hm();
			ent.put(s, t2.getStyle().isBold().orElse(false));
			List<Map.Entry<String, Boolean>> out = Utils.al(ent.entrySet(), true);
			int i2 = ind - s.length();
			for (Text t3 : c) {
				List<Map.Entry<String, Boolean>> c2 = getUpTo(t3, i2);
				out.addAll(c2);
				int l = getl(c2);
				i2 -= l;
				if (i2 == 0) {
					return out;
				}
			}
			return out;
		}
	}

	/**
	 * Return the character at a certain point in the text.
	 */
	public static char charAt(Text t, int i) {
		return getContent(t).charAt(i);
	}

	/**
	 * Compare the formats of two texts.
	 */
	public static boolean formatsEqual(Text one, Text two, boolean strict) {
		if (strict) {
			return one.getFormat().equals(two.getFormat());
		} else {
			boolean c = one.getColor().equals(two.getColor()), s = one.getStyle().equals(two.getStyle());
			if (one.getColor().equals(TextColors.NONE) || two.getColor().equals(TextColors.NONE)) {
				c = true;
			}
			if (one.getStyle().equals(TextStyles.NONE) || two.getStyle().equals(TextStyles.NONE)) {
				s = true;
			}
			return c && s;
		}
	}

	/**
	 * Compare the actions of two texts.
	 */
	public static boolean extrasEqual(Text one, Text two) {
		return one.getClickAction().equals(two.getClickAction()) && one.getHoverAction().equals(two.getHoverAction())
				&& one.getShiftClickAction().equals(two.getShiftClickAction());
	}

	/**
	 * Check to see if the string contents of two texts are equal.
	 */
	public static boolean contentsEqual(Text one, Text two, boolean ignoreCase) {
		String o = getContent(one);
		String t = getContent(two);
		return ignoreCase ? o.equalsIgnoreCase(t) : o.equals(t);
	}

	/**
	 * Checks to see if a text is an instance of a LiteralText
	 */
	public static boolean lit(Text t) {
		return t instanceof LiteralText;
	}

	/**
	 * Remove extra whitespace from a text.
	 */
	public static Text trim(Text t) {
		if (!lit(t)) {
			return t;
		}
		LiteralText.Builder b = ((LiteralText) t).toBuilder();
		return b.content(b.getContent().trim()).build();
	}

	/**
	 * Sets the string content of a text to lower case.
	 */
	public static Text toLowerCase(Text in, Locale locale) {
		if (!lit(in)) {
			return in;
		}
		LiteralText.Builder b = ((LiteralText) in).toBuilder();
		b.content(b.getContent().toLowerCase(locale));
		return b.build();
	}

	/**
	 * Sets the string content of a text to lower case.
	 */
	public static Text toLowerCase(Text in) {
		if (!lit(in)) {
			return in;
		}
		LiteralText.Builder b = ((LiteralText) in).toBuilder();
		b.content(b.getContent().toLowerCase());
		return b.build();
	}

	/**
	 * Sets the string content of a text to upper case.
	 */
	public static Text toUpperCase(Text in, Locale locale) {
		if (!lit(in)) {
			return in;
		}
		LiteralText.Builder b = ((LiteralText) in).toBuilder();
		b.content(b.getContent().toUpperCase(locale));
		return b.build();
	}

	/**
	 * Sets the string content of a text to upper case.
	 */
	public static Text toUpperCase(Text in) {
		if (!lit(in)) {
			return in;
		}
		LiteralText.Builder b = ((LiteralText) in).toBuilder();
		b.content(b.getContent().toUpperCase());
		return b.build();
	}

	/**
	 * Check to see if a text contains a char.
	 */
	public static boolean containsIgnoreCase(Text one, char two) {
		return indexOf(toLowerCase(one), toLowerCase(two)) > -1;
	}

	private static String toLowerCase(String in) {
		return in.toLowerCase();
	}

	private static char toLowerCase(char in) {
		return Character.toLowerCase(in);
	}

	/**
	 * Check to see if a text contains a string.
	 */
	public static boolean containsIgnoreCase(Text one, String two) {
		return indexOf(toLowerCase(one), toLowerCase(two)) > -1;
	}

	/**
	 * Check to see if a text contains another text.
	 */
	public static boolean containsIgnoreCase(Text one, Text two) {
		return indexOf(toLowerCase(one), toLowerCase(two)) > -1;
	}

	/**
	 * Check to see if a text contains a char.
	 */
	public static boolean contains(Text one, char two) {
		return indexOf(one, two) > -1;
	}

	/**
	 * Check to see if a text contains a string.
	 */
	public static boolean contains(Text one, String two) {
		return indexOf(one, two) > -1;
	}

	/**
	 * Check to see if a text contains another text.
	 */
	public static boolean contains(Text one, Text two) {
		return indexOf(one, two) > -1;
	}

	/**
	 * Compare the string contents of two texts.
	 */
	public static int compare(Text one, Text two) {
		return getContent(one).compareTo(getContent(two));
	}

	/**
	 * Check to see if a text ends with another text.
	 */
	public static boolean endsWith(Text in, Text part) {
		return equals(substring(in, length(in) - length(part)), part, true, true, false);
	}

	/**
	 * Check to see if a text ends with a string.
	 */
	public static boolean endsWith(Text in, String part) {
		return getContent(in).endsWith(part);
	}

	/**
	 * Check to see if a text starts with another text.
	 */
	public static boolean startsWith(Text in, Text part) {
		return equals(substring(in, 0, length(part)), part, true, true, false);
	}

	/**
	 * Check to see if a text starts with a string.
	 */
	public static boolean startsWith(Text in, String part) {
		return getContent(in).startsWith(part);
	}

	/**
	 * Check to see if two texts are equal, including click/hover, format and
	 * case.
	 */
	public static boolean equals(Text one, Text two) {
		return equals(one, two, true, true, false);
	}

	/**
	 * Compare two texts.
	 */
	public static boolean equals(Text one, Text two, boolean clickHover, boolean formats, boolean ignoreCase) {
		if (!contentsEqual(one, two, ignoreCase)) {
			return false;
		}
		if (clickHover && !extrasEqual(one, two)) {
			return false;
		}
		if (formats && !formatsEqual(one, two, true)) {
			return false;
		}
		return true;
	}

	/**
	 * Find the index of the val in the text.
	 */
	public static int indexOf(Text in, String val) {
		return indexOf(in, val, 0);
	}

	/**
	 * Find the index of the val in the text after an offset.
	 */
	public static int indexOf(Text in, String val, int offset) {
		if (!lit(in)) {
			return -1;
		}
		return ((LiteralText) in).getContent().indexOf(val, offset);
	}

	/**
	 * Find the index of the val in the text. All of the following must match
	 * for the index to be chosen: Content, format and actions.
	 */
	public static int indexOf(Text in, Text val) {
		return indexOf(in, val, 0);
	}

	/**
	 * Find the index of the val in the text after an offset. All of the
	 * following must match for the index to be chosen: Content, format and
	 * actions.
	 */
	public static int indexOf(Text in, Text val, int offset) {
		if (!lit(in) && !lit(val)) {
			return in.equals(val) ? 0 : -1;
		} else if (!lit(in) || !lit(val)) {
			return -1;
		}
		LiteralText t1 = (LiteralText) in;
		LiteralText t2 = (LiteralText) val;
		int c = t1.getContent().indexOf(t2.getContent(), offset);
		return extrasEqual(t1, t2) && formatsEqual(t1, t2, false) ? c : -1;
	}

	/**
	 * Return the index of the val in the text.
	 */
	public static int indexOf(Text in, char val) {
		return indexOf(in, val, 0);
	}

	/**
	 * Return the index of the val in the text after an offset.
	 */
	public static int indexOf(Text in, char val, int offset) {
		if (!lit(in)) {
			return -1;
		}
		return ((LiteralText) in).getContent().indexOf(val, offset);
	}

	/**
	 * Capitalize the first letter of the text object.
	 */
	public static Text capitalize(Text t) {
		if (!lit(t)) {
			return t;
		}
		LiteralText t1 = (LiteralText) t;
		String content = t1.getContent();
		String capped = Utils.capitalize(content);
		if (capped.equals(content)) {
			List<Text> txs = t1.getChildren();
			int i = 0;
			boolean found = false;
			for (; i < txs.size(); i++) {
				if (!capequalsnorm(txs.get(i))) {
					found = true;
					break;
				}
			}
			List<Text> ot = Utils.al();
			if (found) {
				for (int x = 0; x < txs.size(); x++) {
					if (x == i) {
						ot.add(capitalize(txs.get(x)));
					} else {
						ot.add(txs.get(x));
					}
				}
				return t1.toBuilder().removeAll().append(ot).build();
			} else {
				return t;
			}
		}
		return t1.toBuilder().content(capped).build();
	}

	private static boolean capequalsnorm(Text t) {
		String content = getContent(t);
		String capped = Utils.capitalize(content);
		return content.equals(capped);
	}

	/**
	 * Return the string content of a text. If it's a LiteralText, just
	 * content(). If not, it will return toPlain().
	 */
	public static String getContent(Text t) {
		return t.toPlain();
	}

	/**
	 * Convert a template to a text with vars.
	 */
	public static Text convertToText(TextTemplate template) {
		Map<String, Object> toApply = Utils.hm();
		for (String s : template.getArguments().keySet()) {
			toApply.put(s, "{" + s + "}");
		}
		return template.apply(toApply).build();
	}

	/**
	 * Parse but do not replace vars in a text.
	 */
	public static TextTemplate getVars(Text t) {
		List<Text> all = split(t, Utils.VAR_PATTERN, false);
		List<Object> trans = all.stream().map(text -> {
			if (lit(text)) {
				String content = getContent(text);
				if (Utils.VAR_PATTERN.matcher(content).matches()) {
					String proper = content.replace("{", "").replace("}", "");
					return (Object) TextTemplate.arg(proper).defaultValue(text).format(text.getFormat());
				} else {
					return (Object) text;
				}
			} else {
				return (Object) text;
			}
		}).collect(RayCollectors.rayList());
		return TextTemplate.of(TextTemplate.DEFAULT_OPEN_ARG, TextTemplate.DEFAULT_CLOSE_ARG, trans.toArray());
	}

	/**
	 * Parse a text for variables, and replace them.
	 */
	public static Text vars(Text t, ParsableData data) {
		if (!(t instanceof LiteralText)) {
			return t;
		}
		if (data.getSender().isPresent()) {
			Player s = data.getSender().get();
			if (!s.hasPermission("ray.vars")) {
				return t;
			}
		}
		return parseForVariables((LiteralText) t, data);
	}

	/**
	 * Parse a text for urls and replace them.
	 */
	public static Text urls(Text t) {
		if (!(t instanceof LiteralText)) {
			return t;
		}
		return makeURLClickable((LiteralText) t);
	}

	/**
	 * Clear urls from a text.
	 */
	public static Text noUrls(Text t) {
		if (!(t instanceof LiteralText)) {
			return t;
		}
		return makeURLUnclickable((LiteralText) t);
	}

	private static LiteralText makeURLUnclickable(LiteralText text) {
		return TextUtils.replaceRegex(text, Utils.URL_PATTERN, (matchtext) -> {
			String match = getContent(matchtext);
			return Optional.of((LiteralText) apply(Text.builder(match.replace(".", " ")).format(matchtext.getFormat()),
					matchtext.toBuilder()).build());
		}, true);
	}

	/**
	 * Parse a text for urls and replace them.
	 */
	public static Text urlsIf(Text t) {
		return urls(t);
	}

	/**
	 * Parse a text for urls and replace them.
	 */
	public static LiteralText makeURLClickable(LiteralText text) {
		return TextUtils.replaceRegex(text, Utils.URL_PATTERN, (matchtext) -> {
			String match = getContent(matchtext);
			if (!Utils.toUrlSafe(match).isPresent()) {
				return Optional.of(matchtext);
			}
			return Optional.of(Text.builder(match)
					.format((TextFormat.of(TextColors.BLUE, TextStyles.ITALIC).merge(matchtext.getFormat())))
					.onClick(TextActions.openUrl(Utils.toUrlSafe(match).get()))
					.onHover(TextActions.showText(Text.of(TextColors.AQUA, "Click here to go to " + match + "!")))
					.build());
		}, true);
	}

	/**
	 * Parse a text for variables and replace them.
	 */
	public static LiteralText parseForVariables(LiteralText text, ParsableData data) {
		return TextUtils.replaceRegex(text, Utils.VAR_PATTERN, (matchtext) -> {
			String match = getContent(matchtext);
			String proper = match.replace("{", "").replace("}", "");
			Object out = Ray.get().getVariables().get(proper, data, Optional.empty(), Optional.empty());
			if (data.getKnown().isPresent() && data.getKnown().get().containsKey(proper)) {
				out = data.getKnown().get().get(proper);
			}
			if (out == null) {
				return Optional.empty();
			} else if (out instanceof LiteralText) {
				return Optional.of((LiteralText) out);
			} else if (out instanceof Text) {
				return Optional.empty();
			} else {
				return Optional.of(LiteralText.builder(out.toString()).build());
			}
		}, true);
	}

	private static <T> T replaceRegexAction(TextAction<?> action, Function<T, T> replacer, T def) {
		if (def.getClass().isInstance(action.getResult())) {
			@SuppressWarnings("unchecked")
			T result = (T) action.getResult();
			return replacer.apply(result);
		} else {
			return def;
		}
	}

	private static String format(String a, List<Object> b) {
		return String.format(a, b.toArray(new Object[b.size()]));
	}

	private static LiteralText.Builder bf(String p, LiteralText.Builder builder) {
		LiteralText.Builder tb = LiteralText.builder(p).format(builder.getFormat());
		builder.getClickAction().ifPresent(act -> tb.onClick(act));
		builder.getHoverAction().ifPresent(act -> tb.onHover(act));
		return tb;
	}

	private static String[] allmatches(final String a, final Pattern b) {
		String f = a;
		Matcher m = b.matcher(f);
		List<String> o = Utils.al();
		while ((m = m.reset(f)).find()) {
			o.add(m.group());
			f = m.replaceFirst("");
		}
		return o.toArray(new String[o.size()]);
	}

	/**
	 * Return the length of the content of the text object.
	 */
	public static int length(Text t) {
		return getContent(t).length();
	}

	/**
	 * Parse text colors from a string.
	 */
	public static TextColor colorFrom(String s) {
		if (s == null || s.isEmpty()) {
			return TextColors.NONE;
		}
		TextColor c = fromName(s);
		if (c == TextColors.NONE && s.length() < 3) {
			c = fromString(s);
		}
		return c;
	}

	/**
	 * Parse a text color from a string with formatting codes.
	 */
	public static TextColor fromString(String s) {
		return TextSerializers.FORMATTING_CODE.deserialize(s).getColor();
	}

	/**
	 * Parse a text color from a formatting code.
	 */
	public static TextColor fromChar(char c) {
		return fromString("&" + c + "");
	}

	/**
	 * Parse a text color from a name.
	 */
	public static TextColor fromName(String color) {
		switch (color.toLowerCase().trim()) {
		case "aqua":
			return TextColors.AQUA;
		case "black":
			return TextColors.BLACK;
		case "blue":
			return TextColors.BLUE;
		case "dark_aqua":
		case "dark aqua":
		case "dark cyan":
		case "dark_cyan":
			return TextColors.DARK_AQUA;
		case "dark_blue":
		case "navy":
		case "dark blue":
			return TextColors.DARK_BLUE;
		case "dark_gray":
		case "dark gray":
			return TextColors.DARK_GRAY;
		case "dark_green":
		case "dark green":
			return TextColors.DARK_GREEN;
		case "dark_purple":
		case "dark purple":
			return TextColors.DARK_PURPLE;
		case "dark red":
		case "dark_red":
			return TextColors.DARK_RED;
		case "gold":
		case "orange":
			return TextColors.GOLD;
		case "gray":
			return TextColors.GRAY;
		case "green":
		case "lime":
			return TextColors.GREEN;
		case "purple":
		case "light purple":
		case "light_purple":
			return TextColors.LIGHT_PURPLE;
		case "red":
			return TextColors.RED;
		case "reset":
			return TextColors.RESET;
		case "white":
			return TextColors.WHITE;
		case "yellow":
			return TextColors.YELLOW;
		default:
			return TextColors.NONE;
		}
	}

	/**
	 * Check to see if a text has string content.
	 */
	public static boolean hasContent(final Text text) {
		return !text.toPlain().isEmpty();
	}

	/**
	 * Merge the content of two texts into one text object.
	 */
	public static Text merge(Text one, Text two) {
		if (one == null || two == null) {
			throw new IllegalArgumentException("Texts cannot be null!");
		}
		if (!extrasEqual(one, two) || !formatsEqual(one, two, false)) {
			throw new IllegalArgumentException("Both texts must have matching formats and extras!");
		}
		if (!one.getChildren().isEmpty() || !two.getChildren().isEmpty()) {
			throw new IllegalArgumentException("Texts cannot have children!");
		}
		String val = one.toPlain() + two.toPlain();
		return mergeOnto(one, Text.of(val).toBuilder().format(two.getFormat()).build());
	}

	/**
	 * Merge the children of a text object.
	 */
	public static Text mergeChildrenIfPossible(Text t) {
		if (t.getChildren().isEmpty()) {
			return t;
		}
		List<Text> children = t.getChildren();
		Text cur = t.toBuilder().removeAll().build();
		List<Text> texts = Utils.al();
		for (Text child : children) {
			try {
				cur = merge(cur, child);
			} catch (Exception e) {
				if (child.getChildren().isEmpty()) {
					texts.add(cur);
					cur = child;
				} else {
					try {
						cur = merge(cur, mergeChildrenIfPossible(child));
					} catch (Exception e2) {
						texts.add(cur);
						// assume optional is present due to nature of children.
						// exception throwing is important here anyways
						cur = Utils.getLast(flatten(mergeChildrenIfPossible(child), false)).get();
					}
				}
			}
		}
		Text out = texts.isEmpty() ? cur : texts.get(0);
		boolean f = true;
		for (Text t2 : texts) {
			if (f) {
				f = false;
				continue;
			}
			out = out.concat(t2);
		}
		if (!f) {
			out = out.concat(cur);
		}
		return out;
	}

	/**
	 * Condense children with same formats into singular text objects.
	 */
	public static Text condense(Text t) {
		if (!hasContent(t)) {
			return t;
		}
		if (t.getChildren().isEmpty()) {
			return t;
		}
		Text out = mergeChildrenIfPossible(t);
		if (out.isEmpty()) {
			List<Text> children = out.getChildren().stream().filter(tex -> !tex.isEmpty())
					.collect(RayCollectors.rayList());
			if (children.isEmpty()) {
				return EMPTY();
			}
			Text child = children.get(0);
			boolean f = true;
			for (Text t2 : children) {
				if (f) {
					f = false;
					continue;
				}
				child = child.concat(t2);
			}
			return child;
		} else {
			List<Text> children = out.getChildren();
			return out.toBuilder().removeAll()
					.append(children.stream().filter(tex -> !tex.isEmpty()).collect(RayCollectors.rayList())).build();
		}
	}

	/**
	 * Apply some arguments to a text template.
	 */
	public static TextTemplate applySome(TextTemplate template, Map<String, ?> args) {
		try {
			template.apply(args);
			return TextTemplate.of(template.apply(args));
		} catch (Exception e) {
			List<Object> parts = template.getElements();
			parts = parts.stream().map(obj -> {
				if (obj instanceof TextTemplate.Arg) {
					TextTemplate.Arg a = (Arg) obj;
					if (args.containsKey(a.getName())) {
						return args.get(a.getName());
					} else {
						return (Object) a;
					}
				} else {
					return obj;
				}
			}).collect(RayCollectors.rayList());
			return TextTemplate.of(TextTemplate.DEFAULT_OPEN_ARG, TextTemplate.DEFAULT_CLOSE_ARG, parts.toArray());
		}
	}

	/**
	 * Get a random text color.
	 */
	public static TextColor randomColor() {
		int hex = new Random().nextInt(16);
		String h = Integer.toHexString(hex);
		return TextSerializers.FORMATTING_CODE.deserialize("&" + h).getColor();
	}

}
