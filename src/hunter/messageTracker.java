package hunter;

import org.powerbot.script.*;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Constants;
import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.GroundItem;

import java.util.*;
import java.util.Random;
import java.util.concurrent.Callable;

@Script.Manifest(description = "hrevs hunter script2", name = "hrev hunter2")
public class messageTracker extends PollingScript<ClientContext> implements MessageListener {

    Random seed;

    private int BOX_ITEM = 10008;
    private String BOX_TRAP_OR_ACTIVE = "Box trap";
    private String BOX_CAUGHT = "Shaking box";

    private String RESET_BOX_OPTION = "Reset";
    private String BOX_LAY_OPTION = "Lay";

    private int BOX_RANGE = 10;
    private int MAX_LAYABLE = 3;

    private org.powerbot.script.Tile STARTING_TILE;
    private org.powerbot.script.Tile SOUTHWEST_TILE;
    private org.powerbot.script.Tile SOUTHEAST_TILE;
    private org.powerbot.script.Tile NORTHWEST_TILE;
    private org.powerbot.script.Tile NORTHEAST_TILE;
    private List<org.powerbot.script.Tile> SPOTS = new ArrayList<Tile>();
    private List<org.powerbot.script.Tile> ALL_SPOTS = new ArrayList<Tile>();

    private int TOTAL_BOXES = 0;

    private int BOX_SET_POST_DELAY = 1500;
    private int BOX_SET_NEW_DELAY = 1500;
    private int BOX_RESET_DELAY = 2400;

    private int BOX_SET_POST_DELAY_BURST = 500;
    private int BOX_SET_NEW_DELAY_BURST = 500;
    private int BOX_RESET_DELAY_BURST = 800;
    private int BURST_COUNT = 10;


    private int POLL_DELAY_MIN = 11;
    private int POLL_DELAY_MAX = 391;
    List<MessageEvent> game_messages = new ArrayList<MessageEvent>();

    boolean busy = false;

    private String MESSAGE_BEGIN_SETTING_TRAP = "You begin setting up the trap.";
    private String MESSAGE_CAUGHT_CHINCHOMPA =  "You've caught a carnivorous chinchompa.";
    private String MESSAGE_DISMANTLE_TRAP = "You begin setting up the trap.";

    private List<String> busyify = Arrays.asList(
            MESSAGE_BEGIN_SETTING_TRAP,
            MESSAGE_CAUGHT_CHINCHOMPA,
            MESSAGE_DISMANTLE_TRAP
    );

    private String[] unbusify  = {
    };

    private enum TILE_STATE {
        EMPTY, ACTIVE, CAUGHT, FAILED, DROPPED
    }

    private List<TILE_STATE> tiles_status = new ArrayList<TILE_STATE>();

    @Override
    public void messaged(MessageEvent messageEvent) {
        System.out.println(messageEvent.text());
       // if (busyify.contains( messageEvent.text()) ) busy = true;
        game_messages.add(messageEvent);
    }

    @Override
    public void poll() {
        update_tile_states();
        for (TILE_STATE s: tiles_status
             ) {
            System.out.println(s.toString());
        }
        System.out.println("-------");
    }

    @Override
    public void start() {
        seed = new Random();
        MAX_LAYABLE = (ctx.skills.level(Constants.SKILLS_HUNTER) / 20) + 1;
        TOTAL_BOXES = count_boxes_inv();
        init_tiles();
    }

    @Override
    public void stop() {
        ctx.controller.stop();
        ctx.controller.suspend();
    }

    private void init_tiles(){
        STARTING_TILE = ctx.players.local().tile();
        int x = STARTING_TILE.x();
        int y = STARTING_TILE.y();
        SOUTHEAST_TILE = new Tile(x+1, y-1);
        SOUTHWEST_TILE = new Tile(x-1, y-1);
        NORTHEAST_TILE = new Tile(x+1, y+1);
        NORTHWEST_TILE = new Tile(x-1, y+1);

        ALL_SPOTS.add(STARTING_TILE);
        ALL_SPOTS.add(SOUTHWEST_TILE);
        ALL_SPOTS.add(NORTHWEST_TILE);
        ALL_SPOTS.add(SOUTHEAST_TILE);
        ALL_SPOTS.add(NORTHEAST_TILE);

        for(int i = 0; i < MAX_LAYABLE; i++){
            SPOTS.add(ALL_SPOTS.get(i));
        }
        //Collections.shuffle(SPOTS);
        for(int i = 0 ; i < SPOTS.size(); i++) tiles_status.add(TILE_STATE.EMPTY);
    }

    private void update_tile_states(){

        //list all as case 5, undetecable case
        for(int i = 0; i < tiles_status.size(); i++) tiles_status.set(i, TILE_STATE.EMPTY);

        // case 1, box is in caught stage
        for (org.powerbot.script.rt4.GameObject box : ctx.objects.select().name(BOX_CAUGHT) ){
            if (SPOTS.contains(box.tile())) tiles_status.set(SPOTS.indexOf(box.tile()), TILE_STATE.CAUGHT);
        }

        // case 2,3 box is in failed/active stage
        for (org.powerbot.script.rt4.GameObject box : ctx.objects.select().name(BOX_TRAP_OR_ACTIVE) ){
            if (!SPOTS.contains(box.tile())) continue;
            else if (containsAction(RESET_BOX_OPTION, box)) tiles_status.set(SPOTS.indexOf(box.tile()), TILE_STATE.FAILED);
            else tiles_status.set(SPOTS.indexOf(box.tile()), TILE_STATE.ACTIVE);
        }

        // case 4 dropped box
        for (org.powerbot.script.rt4.GroundItem box : ctx.groundItems.select().name(BOX_TRAP_OR_ACTIVE) ) {
            if (SPOTS.contains(box.tile())) tiles_status.set(SPOTS.indexOf(box.tile()), TILE_STATE.DROPPED);
        }
    }

    private int count_boxes_inv(){ return ctx.inventory.select().name(BOX_TRAP_OR_ACTIVE).size(); }

    private String latest_activity(){
        for(int i = game_messages.size()-1; i >= 0; i--){
            String msg = game_messages.get(i).text();
            if (busyify.contains(msg)) return msg;
        }
        return "";
    }

    private void wait_for_trap_set(final Tile tile){
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return latest_activity() == MESSAGE_BEGIN_SETTING_TRAP &&
                        object_exists_at(tile);
            }
        },300,10);
    }

    private boolean object_exists_at(Tile tile){
        for (org.powerbot.script.rt4.GameObject box : ctx.objects.select().name(BOX_TRAP_OR_ACTIVE) ){
            if (box.tile() == tile && !containsAction(RESET_BOX_OPTION, box)) return true;
        }
        return false;
    }

    private boolean in_range(GameObject obj, int range){
        return obj.tile().distanceTo(ctx.players.local()) <= range;
    }

    private boolean in_range(GroundItem obj , int range){
        return obj.tile().distanceTo(ctx.players.local()) <= range;
    }

    private boolean containsAction(String action, GameObject sceneObject){
        return sceneObject==null ? false : Arrays.asList(sceneObject.actions()).contains(action);
    }
    private boolean containsAction(String action, GroundItem sceneObject){
        return sceneObject==null ? false : Arrays.asList(sceneObject.actions()).contains(action);
    }
}
