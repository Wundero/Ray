package me.Wundero.Ray.commands;
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

import java.util.List;
import java.util.Map;

import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;

import me.Wundero.Ray.utils.Utils;

/**
 * Child commands of /ray
 */
public class Commands {

	private static Map<List<String>, CommandCallable> children = Utils.hm();
	static {
		children.put(Utils.al("format"), CommandSpec.builder().permission("ray.formatbuilder")
				.description(Text.of("Create a new format.")).executor(new FormatConversationCommand()).build());
		children.put(Utils.al("announce"),
				CommandSpec.builder().permission("ray.announcementbuilder")
						.description(Text.of("Create a new announcement"))
						.executor(new AnnouncementConversationCommand()).build());
		children.put(Utils.al("channel"), CommandSpec.builder().permission("ray.channelbuilder")
				.description(Text.of("Create a new channel")).executor(new ChannelConversationCommand()).build());

		children.put(Utils.al("test-translate"),
				CommandSpec.builder().permission("ray.translate.test").executor(new TranslationTestCommand())
						.arguments(GenericArguments.string(Text.of("key")),
								GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("args"))))
						.description(Text.of("Test translations.")).build());
		children.put(Utils.al("lock"),
				CommandSpec.builder().permission("ray.chatlock").executor(new ChatLockCommand())
						.description(Text.of("Lock chat messages"))
						.arguments(GenericArguments.optional(GenericArguments.bool(Text.of("value")))).build());
		children.put(Utils.al("filter"),
				CommandSpec.builder().arguments(GenericArguments.optional(GenericArguments.bool(Text.of("value"))))
						.executor(new ChatFilterCommand()).permission("ray.chatfilter")
						.description(Text.of("Filter chat messages")).build());
	}

	/**
	 * Get all children
	 */
	public static Map<List<String>, ? extends CommandCallable> getChildren() {
		return children;
	}

	/**
	 * Get main command
	 */
	public static CommandExecutor getExecutor() {
		return new RayCommand();
	}
}
