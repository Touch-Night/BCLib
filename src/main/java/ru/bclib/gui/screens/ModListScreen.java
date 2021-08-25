package ru.bclib.gui.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import ru.bclib.api.dataexchange.handler.autosync.HelloClient;
import ru.bclib.gui.gridlayout.GridColumn;
import ru.bclib.gui.gridlayout.GridLayout;
import ru.bclib.gui.gridlayout.GridRow;
import ru.bclib.gui.gridlayout.GridScreen;
import ru.bclib.util.ModUtil;
import ru.bclib.util.Pair;
import ru.bclib.util.PathUtil;
import ru.bclib.util.Triple;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class ModListScreen extends BCLibScreen {

    private final List<ModUtil.ModInfo> mods;
    private final HelloClient.IServerModMap serverInfo;
    private final Component description;

    private static List<ModUtil.ModInfo> extractModList(Map<String, ModUtil.ModInfo> mods){
        List<ModUtil.ModInfo> list = new LinkedList<ModUtil.ModInfo>();
        ModUtil.getMods().forEach((id, info) -> list.add(info));
        return list;
    }

    public ModListScreen(Screen parent, Component title, Component description, Map<String, ModUtil.ModInfo> mods, HelloClient.IServerModMap serverInfo) {
       this(parent, title, description, extractModList(mods), serverInfo);
    }

    public ModListScreen(Screen parent, Component title, Component description, List<ModUtil.ModInfo> mods, HelloClient.IServerModMap serverInfo) {
        super(parent, title, 10, true);
        this.mods = mods;
        this.serverInfo = serverInfo;
        this.description = description;
    }

    public static void addModDesc(GridColumn grid, java.util.List<ModUtil.ModInfo> mods, HelloClient.IServerModMap serverInfo, GridScreen parent) {
        final int STATE_OK = 0;
        final int STATE_MISSING = 1;
        final int STATE_SERVER_MISSING = 2;
        final int STATE_VERSION = 3;
        final int STATE_SERVER_MISSING_CLIENT_MOD = 4;

        List<Triple<String, Integer, String>> items = new LinkedList<>();
        if (serverInfo!=null) {
            serverInfo.keySet()
                    .stream()
                    .filter(modid -> !mods.stream().filter(mod -> mod.metadata.getId().equals(modid)).findFirst().isPresent())
                    .forEach(modid -> {
                        int size =  serverInfo.get(modid).second;
                        String stateString = serverInfo.get(modid).first;
                        if (size>0) {
                            stateString = "Version: " + stateString + ", Size: " + PathUtil.humanReadableFileSize(size);
                        }

                        items.add(new Triple<>(modid, STATE_MISSING, stateString));
                    });
        }

        mods.forEach(mod -> {
            String serverVersion = null;
            int serverSize = 0;
            int state = STATE_OK;
            if (serverInfo != null) {
                final String modID = mod.metadata.getId();


                Pair<String, Integer> data = serverInfo.get(modID);
                if (data!=null) {
                    final String modVer = data.first;
                    final int size = data.second;
                    if (!modVer.equals(mod.getVersion())) {
                        state = STATE_VERSION;
                        serverVersion = modVer;
                        serverSize = size;
                    }
                } else if (mod.metadata.getEnvironment() == ModEnvironment.CLIENT){
                    state = STATE_SERVER_MISSING_CLIENT_MOD;
                } else {
                    state = STATE_SERVER_MISSING;
                }
            }

            String stateString = mod.metadata.getVersion().toString();
            if (serverVersion!=null) {
                stateString = "Client: " + stateString;
                stateString += ", Server: " + serverVersion;
                if (serverSize>0) {
                    stateString += ", Size: " + PathUtil.humanReadableFileSize(serverSize);
                }
            }
            if (mod.metadata.getEnvironment() == ModEnvironment.CLIENT) {
                stateString+= ", client-only";
            } else if (mod.metadata.getEnvironment() == ModEnvironment.SERVER) {
                stateString+= ", server-only";
            }
            items.add(new Triple<>(mod.metadata.getName(), state, stateString));
        });

        items.stream()
                .sorted(Comparator.comparing(a -> a.first.toLowerCase(Locale.ROOT)))
                .forEach(t -> {
                    final String name = t.first;
                    final int state = t.second;
                    final String stateString = t.third;

                    int color = GridLayout.COLOR_RED;
                    final String typeText;
                    if (state==STATE_VERSION) {
                        typeText = "[VERSION]";
                    } else if (state==STATE_MISSING) {
                        typeText = "[MISSING]";
                    } else if (state==STATE_SERVER_MISSING || state == STATE_SERVER_MISSING_CLIENT_MOD) {
                        typeText = "[NOT ON SERVER]";
                        if (state == STATE_SERVER_MISSING_CLIENT_MOD) {
                            color = GridLayout.COLOR_YELLOW;
                        }
                    } else {
                        color = GridLayout.COLOR_CYAN;
                        typeText = "[OK]";
                    }
                    TextComponent dash = new TextComponent("-");
                    TextComponent typeTextComponent = new TextComponent(typeText);
                    GridRow row = grid.addRow();

                    row.addString(dash, parent);

                    row.addSpacer(4);
                    row.addString(new TextComponent(name), parent);

                    row.addSpacer(4);
                    row.addString(typeTextComponent, color, parent);

                    if (!stateString.isEmpty()) {
                        row = grid.addRow();
                        row.addSpacer(4 + parent.getWidth(dash));
                        row.addString(new TextComponent(stateString), GridLayout.COLOR_GRAY, parent);
                    }

                    grid.addSpacerRow();
                });
    }

    @Override
    protected void initLayout() {
        if (description != null) {
            grid.addSpacerRow();
            grid.addRow().addMessage(description, font, GridLayout.Alignment.CENTER);
            grid.addSpacerRow(8);
        }

        GridRow row = grid.addRow();
        row.addSpacer(10);
        GridColumn col = row.addColumn(200, GridLayout.GridValueType.CONSTANT);
        addModDesc(col, mods, serverInfo, this);

        grid.addSpacerRow(8);
        row = grid.addRow();
        row.addFiller();
        row.addButton(CommonComponents.GUI_BACK, 20, font, (n)-> {
            onClose();
            System.out.println("Closing");
        });
        row.addFiller();
    }

}
