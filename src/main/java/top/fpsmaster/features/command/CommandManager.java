package top.fpsmaster.features.command;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventSendChatMessage;
import top.fpsmaster.features.impl.interfaces.BlockIndicator;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.features.impl.interfaces.PlayTime;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.utils.core.Utility;

import java.util.ArrayList;
import java.util.List;

import static top.fpsmaster.utils.core.Utility.mc;

public class CommandManager {

    private final List<Command> commands = new ArrayList<>();

    public void init() {
        // add commands
        commands.add(new Command("blockindicator") {
            @Override
            public void execute(String[] args) {
                setModuleState(BlockIndicator.class, args);
            }
        });
        commands.add(new Command("bi") {
            @Override
            public void execute(String[] args) {
                setModuleState(BlockIndicator.class, args);
            }
        });
        commands.add(new Command("playtime") {
            @Override
            public void execute(String[] args) {
                setModuleState(PlayTime.class, args);
            }
        });
        commands.add(new Command("pt") {
            @Override
            public void execute(String[] args) {
                setModuleState(PlayTime.class, args);
            }
        });
        EventDispatcher.registerListener(this);
    }

    @Subscribe
    public void onChat(EventSendChatMessage e) throws Exception {
        String prefix = ClientSettings.prefix.getValue();
        if (ClientSettings.clientCommand.getValue() && e.msg.startsWith(prefix)) {
            e.cancel();
            mc.ingameGUI.getChatGUI().addToSentMessages(e.msg);
            if (e.msg.length() <= prefix.length()) {
                return;
            }
            runCommand(e.msg.substring(prefix.length()));

        }
    }

    private void runCommand(String command) throws Exception {
        String[] args = command.split(" ");
        String cmd = args[0];
        if (args.length == 1) {
            for (Command commandItem : commands) {
                if (commandItem.name.equals(cmd)) {
                    commandItem.execute(new String[]{});
                    return;
                }
            }
            Utility.sendClientMessage(FPSMaster.i18n.get("command.notfound"));
            return;
        }
        String[] cmdArgs = new String[args.length - 1];
        System.arraycopy(args, 1, cmdArgs, 0, cmdArgs.length);
        for (Command commandItem : commands) {
            if (commandItem.name.equals(cmd)) {
                commandItem.execute(cmdArgs);
                return;
            }
        }
        Utility.sendClientMessage(FPSMaster.i18n.get("command.notfound"));
    }

    private void setModuleState(Class<? extends Module> moduleClass, String[] args) {
        if (args.length == 0) {
            toggleModule(moduleClass);
            return;
        }
        Module module = FPSMaster.moduleManager.getModule(moduleClass);
        String action = args[0].toLowerCase();
        if ("on".equals(action) || "enable".equals(action) || "true".equals(action) || "开启".equals(action)) {
            module.set(true);
            sendModuleState(module);
            return;
        }
        if ("off".equals(action) || "disable".equals(action) || "false".equals(action) || "关闭".equals(action)) {
            module.set(false);
            sendModuleState(module);
            return;
        }
        Utility.sendClientMessage(FPSMaster.i18n.get("command.notfound"));
    }

    private void toggleModule(Class<? extends Module> moduleClass) {
        Module module = FPSMaster.moduleManager.getModule(moduleClass);
        module.toggle();
        sendModuleState(module);
    }

    private void sendModuleState(Module module) {
        Utility.sendClientNotify(FPSMaster.i18n.get(module.name.toLowerCase()) + ": " +
                FPSMaster.i18n.get(module.isEnabled() ? "command.module.enabled" : "command.module.disabled"));
    }
}



