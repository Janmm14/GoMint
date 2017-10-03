package io.gomint.server.command;

import io.gomint.command.CommandExecutor;
import io.gomint.command.CommandOverload;
import io.gomint.server.entity.CommandPermission;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Set;

/**
 * @author geNAZt
 */
@AllArgsConstructor
@Getter
public class CommandHolder {

    private String name;
    private String description;
    private Set<String> alias;

    private CommandPermission commandPermission;
    private String permission;
    private CommandExecutor executor;
    private List<CommandOverload> overload;

}
