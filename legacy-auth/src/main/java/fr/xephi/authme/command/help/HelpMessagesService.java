package fr.xephi.authme.command.help;

import com.google.common.base.CaseFormat;
import fr.xephi.authme.command.CommandArgumentDescription;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandUtils;
import fr.xephi.authme.message.HelpMessagesFileHandler;
import fr.xephi.authme.permission.DefaultPermission;

import javax.inject.Inject;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Manages translatable help messages.
 */
public class HelpMessagesService {

    private static final String COMMAND_PREFIX = "commands.";
    private static final String DESCRIPTION_SUFFIX = ".description";
    private static final String DETAILED_DESCRIPTION_SUFFIX = ".detailedDescription";
    private static final String DEFAULT_PERMISSIONS_PATH = "common.defaultPermissions.";

    private final HelpMessagesFileHandler helpMessagesFileHandler;

    @Inject
    HelpMessagesService(HelpMessagesFileHandler helpMessagesFileHandler) {
        this.helpMessagesFileHandler = helpMessagesFileHandler;
    }

    /**
     * Creates a copy of the supplied command description with localized messages where present.
     *
     * @param command the command to build a localized version of
     * @return the localized description
     */
    public CommandDescription buildLocalizedDescription(CommandDescription command) {
        return buildLocalizedDescription(command, null);
    }

    /**
     * Creates a copy of the supplied command description with messages localized in the given
     * language.
     *
     * @param command the command to build a localized version of
     * @param language the language to localize the command in, null for the configured language
     * @return the localized description
     */
    public CommandDescription buildLocalizedDescription(CommandDescription command, String language) {
        final String path = COMMAND_PREFIX + getCommandSubPath(command);
        if (!helpMessagesFileHandler.hasSection(path, language)) {
            // Messages file does not have a section for this command - return the provided command
            return command;
        }

        CommandDescription.CommandBuilder builder = CommandDescription.builder()
            .description(getText(path + DESCRIPTION_SUFFIX, language, command::getDescription))
            .detailedDescription(
                getText(path + DETAILED_DESCRIPTION_SUFFIX, language, command::getDetailedDescription))
            .executableCommand(command.getExecutableCommand())
            .parent(command.getParent())
            .labels(command.getLabels())
            .permission(command.getPermission());

        int i = 1;
        for (CommandArgumentDescription argument : command.getArguments()) {
            String argPath = path + ".arg" + i;
            String label = getText(argPath + ".label", language, argument::getName);
            String description = getText(argPath + ".description", language, argument::getDescription);
            builder.withArgument(label, description, argument.isOptional());
            ++i;
        }

        CommandDescription localCommand = builder.build();
        localCommand.getChildren().addAll(command.getChildren());
        return localCommand;
    }

    public String getDescription(CommandDescription command) {
        return getDescription(command, null);
    }

    /**
     * Returns the description of the given command in the given language.
     *
     * @param command the command to get the description of
     * @param language the language to use, null for the configured language
     * @return the command description
     */
    public String getDescription(CommandDescription command, String language) {
        return getText(COMMAND_PREFIX + getCommandSubPath(command) + DESCRIPTION_SUFFIX, language,
            command::getDescription);
    }

    public String getMessage(HelpMessage message) {
        return getMessage(message, null);
    }

    /**
     * Returns the given help message in the given language.
     *
     * @param message the message to retrieve
     * @param language the language to use, null for the configured language
     * @return the message
     */
    public String getMessage(HelpMessage message, String language) {
        return helpMessagesFileHandler.getMessage(message.getKey(), language);
    }

    public String getMessage(HelpSection section) {
        return getMessage(section, null);
    }

    /**
     * Returns the given help section title in the given language.
     *
     * @param section the section to retrieve the title of
     * @param language the language to use, null for the configured language
     * @return the section title
     */
    public String getMessage(HelpSection section, String language) {
        return helpMessagesFileHandler.getMessage(section.getKey(), language);
    }

    public String getMessage(DefaultPermission defaultPermission) {
        return getMessage(defaultPermission, null);
    }

    /**
     * Returns the description of the given default permission in the given language.
     *
     * @param defaultPermission the default permission to describe
     * @param language the language to use, null for the configured language
     * @return the description
     */
    public String getMessage(DefaultPermission defaultPermission, String language) {
        // e.g. {default_permissions_path}.opOnly for DefaultPermission.OP_ONLY
        String path = DEFAULT_PERMISSIONS_PATH + getDefaultPermissionsSubPath(defaultPermission);
        return helpMessagesFileHandler.getMessage(path, language);
    }

    public static String getDefaultPermissionsSubPath(DefaultPermission defaultPermission) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, defaultPermission.name());
    }

    private String getText(String path, String language, Supplier<String> defaultTextGetter) {
        String message = helpMessagesFileHandler.getMessageIfExists(path, language);
        if (message == null) {
            message = helpMessagesFileHandler.getMessageIfExists(path);
        }
        return message == null
            ? defaultTextGetter.get()
            : message;
    }


    /**
     * Triggers a reload of the help messages file. Note that this method is not needed
     * to be called for /authme reload.
     */
    public void reloadMessagesFile() {
        helpMessagesFileHandler.reload();
    }

    /**
     * Returns the command subpath for the given command (i.e. the path to the translations for the given
     * command under "commands").
     *
     * @param command the command to process
     * @return the subpath for the command's texts
     */
    public static String getCommandSubPath(CommandDescription command) {
        return CommandUtils.constructParentList(command)
            .stream()
            .map(cmd -> cmd.getLabels().get(0))
            .collect(Collectors.joining("."));
    }
}
