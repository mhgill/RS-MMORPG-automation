package hunter; //CHANGE TO YOUR PACKAGE NAME

import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.powerbot.script.*;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.GroundItem;
import org.powerbot.script.rt4.GroundItems;
import org.powerbot.script.rt4.Objects;
import org.powerbot.script.*;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Game;
import org.powerbot.script.rt4.Item;
import org.powerbot.script.rt4.Npc;
import org.powerbot.script.rt4.*;


import java.util.concurrent.Callable;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import java.util.Timer;
import java.util.Random;

@Script.Manifest(description = "hrevs hunter script", name = "hrev hunter")
public class trapper extends PollingScript<ClientContext>{ //CHANGE TO YOUR CLASS NAME

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

    messageTracker messages;

    @Override
    public void start(){
        seed = new Random();
        MAX_LAYABLE = (ctx.skills.level(Constants.SKILLS_HUNTER) / 20) + 1;
        TOTAL_BOXES = count_boxes_inv();
        init_tiles();
        messages = new messageTracker();
    }

    @Override
    public void poll() {
        /*
        check_and_reset_traps();
        //set_new_traps();
        wait(random(POLL_DELAY_MIN, POLL_DELAY_MAX));*/
    }

    private void check_and_reset_traps(){
        // for caught
        for (org.powerbot.script.rt4.GameObject box : ctx.objects.select().name(BOX_CAUGHT, BOX_TRAP_OR_ACTIVE) ){
            if (in_range(box, BOX_RANGE) && containsAction(RESET_BOX_OPTION, box)) {
                System.out.println("resetting caught box");
                box.interact(RESET_BOX_OPTION);
                post_reset_delay(box.tile());
            }
        }
        // for dropped/expired boxes
        for (org.powerbot.script.rt4.GroundItem box : ctx.groundItems.select().name(BOX_TRAP_OR_ACTIVE) ){
            if (in_range(box, BOX_RANGE) && containsAction(BOX_LAY_OPTION, box) && containsAction("Take", box)) {
                System.out.println("resetting dropped box");
                box.interact(BOX_LAY_OPTION);
                post_droppped_delay();
            }
        }
       /* if (TOTAL_BOXES - count_boxes_inv() < MAX_LAYABLE
                && active_boxes() + caught_boxes() + dropped_boxes() < MAX_LAYABLE
                ) set_new_traps();*/
    }

    private int total_boxes(){
       return
               ctx.inventory.select().name(BOX_TRAP_OR_ACTIVE).size()
                       + active_boxes() + dropped_boxes() + caught_boxes();
    }

    private int count_boxes_inv(){
        return
                ctx.inventory.select().name(BOX_TRAP_OR_ACTIVE).size()
                   ;
    }




    private int random(int low, int high){
        return seed.nextInt(high-low) + low;
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
        ALL_SPOTS.add(NORTHWEST_TILE);
        ALL_SPOTS.add(SOUTHEAST_TILE);
        ALL_SPOTS.add(SOUTHWEST_TILE);
        ALL_SPOTS.add(NORTHEAST_TILE);

        for(int i = 0; i < MAX_LAYABLE; i++){
            SPOTS.add(ALL_SPOTS.get(i));
        }

        Collections.shuffle(SPOTS);
    }

    private void wait(int ms){
        try { TimeUnit.MILLISECONDS.sleep(ms/2); }
        catch (Exception e) { }

        try { ctx.controller.wait(ms/2);}
        catch (Exception e) { }
    }

    private void set_new_traps(){
        for(int i = 0 ; i < MAX_LAYABLE; i++){
            lay_new_trap(SPOTS.get(i));
        }
    }

    private boolean box_on_tile(Tile tile){
        for (org.powerbot.script.rt4.GameObject box : ctx.objects.select().name(BOX_CAUGHT, BOX_TRAP_OR_ACTIVE) ){
            if (box.tile().y() == tile.y() && box.tile().x() == tile.x()) return true;
        }
        for (org.powerbot.script.rt4.GroundItem box : ctx.groundItems.select().name(BOX_TRAP_OR_ACTIVE) ){
            if (box.tile().y() == tile.y() && box.tile().x() == tile.x()) return true;
        }
        return false;
    }

    @Override
    public void stop(){
        ctx.controller.suspend();
    }


    /*
      private void step(Tile tile){
        ctx.movement.step(tile);
        int m = 10;
        while ( random(0, m) < m/2 )
        {
            ctx.movement.step(tile);
            m *= 2;
        }
    }
     */

    private void lay_new_trap(final Tile tile){
        if (box_on_tile(tile)) return;

        ctx.movement.step(tile);
        ctx.movement.step(tile);
        ctx.movement.step(tile);

        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.players.local().tile().x() == tile.x() &&
                        ctx.players.local().tile().y() == tile.y();
            }
        }, BOX_SET_NEW_DELAY_BURST, BURST_COUNT);
        wait(BOX_SET_NEW_DELAY);

        if (!(ctx.players.local().tile().x() == tile.x() &&
                ctx.players.local().tile().y() == tile.y())) return;

        ctx.inventory.select().name(BOX_TRAP_OR_ACTIVE).poll().interact(BOX_LAY_OPTION);
        post_new_delay(tile);
    }

    private void post_new_delay(final Tile tile){
        //final int i = count_non_dropped_traps();
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                    if (ctx.players.local().inMotion()) return false; // remove if making too slow
                    for (GroundItem box : ctx.groundItems.select().name(BOX_TRAP_OR_ACTIVE)) {
                        if (box.tile().x() == tile.x() && box.tile().y() == tile.y()) return true;
                    }
                    return false;
            }
        }, BOX_SET_POST_DELAY_BURST, BURST_COUNT);
        wait(BOX_SET_POST_DELAY);
    }

    private void post_droppped_delay(){
        final int i = dropped_boxes();
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return dropped_boxes() < i && !ctx.players.local().inMotion();
            }
        }, BOX_SET_NEW_DELAY_BURST,BURST_COUNT);
        wait(BOX_SET_NEW_DELAY);
    }

    private int dropped_boxes(){
        int s = 0;
        for (org.powerbot.script.rt4.GroundItem box : ctx.groundItems.select().name( BOX_TRAP_OR_ACTIVE) ){
            if (in_range(box, BOX_RANGE) ) s++;
        }
        return s;
    }

    private void post_reset_delay(final Tile tile){
       // final int i = active_boxes();
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (ctx.players.local().inMotion()) return false; // remove if making too slow
                for (GameObject box : ctx.objects.select().name(BOX_TRAP_OR_ACTIVE)) {
                    if (box.tile().x() == tile.x() && box.tile().y() == tile.y()) return true;
                }
                return false;
            }
        }, BOX_RESET_DELAY_BURST, BURST_COUNT);
        wait(BOX_RESET_DELAY);
    }

    private int count_non_dropped_traps(){
        int s = 0;
        for (org.powerbot.script.rt4.GameObject box : ctx.objects.select().name(BOX_CAUGHT, BOX_TRAP_OR_ACTIVE) ){
            if (in_range(box, BOX_RANGE)) s++;
        }
        return s;
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
    private int boxes_on_ground(){
        int c = 0;
        for (org.powerbot.script.rt4.GameObject box : ctx.objects.select().name(BOX_CAUGHT, BOX_TRAP_OR_ACTIVE) ){
            if (in_range(box, BOX_RANGE)) c++;
        }
        for (org.powerbot.script.rt4.GroundItem box : ctx.groundItems.select().name(BOX_TRAP_OR_ACTIVE) ){
            if (in_range(box, BOX_RANGE)) c++;
        }
        return c;
    }
    private int active_boxes(){
        int c = 0;
        for (org.powerbot.script.rt4.GameObject box : ctx.objects.select().name(BOX_TRAP_OR_ACTIVE) ){
            if (in_range(box, BOX_RANGE) && !containsAction(RESET_BOX_OPTION, box)) c++;
        }
        return c;
    }
    private int caught_boxes(){
        int c = 0;
        for (org.powerbot.script.rt4.GameObject box : ctx.objects.select().name(BOX_CAUGHT) ){
            if (in_range(box, BOX_RANGE) && !containsAction(RESET_BOX_OPTION, box)) c++;
        }
        return c;
    }

}