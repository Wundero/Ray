package me.Wundero.Ray.conversation;
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

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

/**
 * Options to be chosen from in a conversation
 */
public class Option {
	private final Text display;
	private final String key;
	private final Object value;

	/**
	 * Create an option
	 */
	public Option(String key, Text display, Object value) {
		this.display = display;
		this.key = key;
		this.value = value;
	}

	/**
	 * Check to see if the option is what matches the input
	 */
	public boolean works(String input) {
		String k2 = key.toLowerCase().trim();
		String i2 = input.toLowerCase().trim();
		return k2.equals(i2);
	}

	/**
	 * Get the text to be sent to the player
	 */
	public Text getDisplay() {
		return display;
	}

	/**
	 * Get the name of the option
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get the value stored by the option
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Create an option with a default Text object around a key value entry.
	 */
	public static Option build(String name, Object value) {
		return new Option(name,
				Text.builder(name).color(TextColors.GOLD).onClick(TextActions.runCommand(name))
						.onHover(TextActions.showText(Text.of(TextColors.AQUA, "Click to select " + name + "!")))
						.build(),
				value);
	}
}
