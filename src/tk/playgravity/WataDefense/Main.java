package tk.playgravity.WataDefense;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Main extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private List<String> red_team_str_list;
    private List<String> blue_team_str_list;
    private boolean actual_game_started = false;

    private int red_block_x;
    private int blue_block_x;

    private int red_spawn_y;
    private int blue_spawn_y;
    private Block blue_bedrock;
    private Block red_bedrock;

    private World the_world;

    private void run_as_console(String command){
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        Block interacted_block = event.getClickedBlock();
        if (interacted_block != null) {
            if (interacted_block.getZ() == 8) {
                int block_x = interacted_block.getX();
                boolean red_block_interacted = block_x == red_block_x;
                boolean blue_block_interacted = block_x == blue_block_x;
                if (red_block_interacted) {
                    red_block_interacted = interacted_block.getY() == red_spawn_y;
                } else if (blue_block_interacted) {
                    blue_block_interacted = interacted_block.getY() == blue_spawn_y;
                } else {
                    return;
                }
                if (red_block_interacted || blue_block_interacted) {
                    Player interactor = event.getPlayer();
                    Set<String> interactor_tags = interactor.getScoreboardTags();
                    if (interactor_tags.contains("bedrock")) {
                        if (red_block_interacted && red_team_str_list.contains(interactor.getName()) && interacted_block.getType() == Material.BEDROCK) {
                            //RED TEAM WINS!
                            run_as_console("minecraft:title @a title {" + '"' + "text" + '"' + ":" + '"' + "RED TEAM WON!" + '"' + "}");
                            run_as_console("minecraft:gamemode spectator @a");
                        } else if (blue_block_interacted && blue_team_str_list.contains(interactor.getName()) && interacted_block.getType() == Material.BEDROCK) {
                            //BLUE TEAM WINS!
                            run_as_console("minecraft:title @a title {" + '"' + "text" + '"' + ":" + '"' + "BLUE TEAM WON!" + '"' + "}");
                            run_as_console("minecraft:gamemode spectator @a");
                        }
                    } else if ((red_block_interacted && blue_team_str_list.contains(interactor.getName()) && interacted_block.getType() == Material.BEDROCK) || (blue_block_interacted && red_team_str_list.contains(interactor.getName()) && interacted_block.getType() == Material.BEDROCK)) {
                        interactor.setPlayerListName("BR " + interactor.getName());
                        interacted_block.setType(Material.STRUCTURE_BLOCK);
                        run_as_console("minecraft:title @a title {" + '"' + "text" + '"' + ":" + '"' + "BEDROCK OBTAINED!" + '"' + "}");
                        interactor.addScoreboardTag("bedrock");
                    } else {
                        interactor.sendMessage("Go break the other teams' bedrock");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Set<String> player_tags = player.getScoreboardTags();
        if (player_tags.contains("bedrock")) {
            //Player had bedrock
            player.setPlayerListName(player.getName());
            player.removeScoreboardTag("bedrock");
            if (red_team_str_list.contains(player.getName())) {
                //Player is from red team. Had blue's bedrock. We have to restore it.
                blue_bedrock.setType(Material.BEDROCK);
            } else {
                //Player is from blue team. Because spectators can't die or get bedrock tag. Had red's bedrock. We have to restore it
                red_bedrock.setType(Material.BEDROCK);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!actual_game_started) {
            List<String> connected_players_names = new ArrayList<String>();
            for (Player a_player : Bukkit.getOnlinePlayers()) {
                connected_players_names.add(a_player.getName());
            }

            List<String> actual_players_names = new ArrayList<String>(red_team_str_list);
            actual_players_names.addAll(blue_team_str_list);

            if (connected_players_names.containsAll(actual_players_names)) {
                //Let the game begin!
                this.getLogger().info("Starting game in 20 seconds!");
                int timer;
                for (timer = 0; timer <= 400; timer += 20) {
                    actual_game_started = true;
                    int time = 10 - (timer / 20);
                    Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                        @Override
                        public void run() {
                            //Show countdown!
                            run_as_console("minecraft:title @a title {" + '"' + "text" + '"' + ":" + '"' + time + '"' + "}");
                        }
                    }, timer);
                }

                timer += 5;

                int red_spawn_y = the_world.getHighestBlockYAt(red_block_x, 8);
                int blue_spawn_y = the_world.getHighestBlockYAt(blue_block_x, 8);


                Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                    @Override
                    public void run() {
                        //TP players, set gamemodes etc. Proper beginning of the GAME!
                        //First set spawn point
                        run_as_console("minecraft:spawnpoint @a[team=red] " + red_block_x + " " + red_spawn_y + " 8");
                        run_as_console("minecraft:spawnpoint @a[team=blue] " + blue_block_x + " " + blue_spawn_y + " 8");

                        //Then, tp player
                        run_as_console("minecraft:tp @a[team=red] " + red_block_x + " " + red_spawn_y + " 8");
                        run_as_console("minecraft:tp @a[team=blue] " + blue_block_x + " " + blue_spawn_y + " 8");

                        //Then, set gamemode
                        run_as_console("minecraft:gamemode survival @a[team=!]");

                        //Report Block location
                        run_as_console("minecraft:title @a[team=!] times 0 140 20");
                        run_as_console("minecraft:title @a[team=red] title {" + '"' + "text" + '"' + ":" + '"' + "Block at X = " + blue_block_x + ", Z = 8" + '"' + "}");
                        run_as_console("minecraft:title @a[team=blue] title {" + '"' + "text" + '"' + ":" + '"' + "Block at X = " + red_block_x + ", Z = 8" + '"' + "}");
                    }
                }, timer);

                int time_before_wall_destruction = config.getInt("walltime");
                int wall_timer = time_before_wall_destruction + timer;

                Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                    @Override
                    public void run() {
                        destroy_wall();
                    }
                }, wall_timer);

            }
        }
    }

    private void destroy_wall() {
        int border_distance_from_block = config.getInt("border_distance");


        int play_area_z_min = blue_block_x - border_distance_from_block - 16;
        int play_area_z_max = red_block_x + border_distance_from_block + 16;

        int timer = 10;
        for (int z = play_area_z_min; z <= play_area_z_max; z += 16) {
            Location spot = new Location(the_world, 8, 10, z);
            Chunk the_chunk = the_world.getChunkAt(spot);

            Block min_block = the_chunk.getBlock(7, 62, 0);
            Block max_block = the_chunk.getBlock(8, 78, 15);

            int x_min = min_block.getX();
            int y_min = min_block.getY();
            int z_min = min_block.getZ();

            int x_max = max_block.getX();
            int y_max = max_block.getY();
            int z_max = max_block.getZ();

            String fill_commnd = "minecraft:fill " + x_min + " " + y_min + " " + z_min + " " + x_max + " " + y_max + " " + z_max + " minecraft:air 0";

            Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                @Override
                public void run() {
                    the_chunk.load();
                    run_as_console(fill_commnd);
                    the_chunk.unload(true);
                }
            }, timer);

            timer += 2;
        }

        //Wall completion Message
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("WALL PARTIALLY DESTROYED!");
            }
        }, timer);

    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        config = this.getConfig();

        boolean first_run = config.getBoolean("first_run");

        if(first_run) {
            getServer().getPluginManager().disablePlugin(this);
        }

        getServer().getPluginManager().registerEvents(this, this);

        String world_name = config.getString("world_name");

        int block_distance_from_center = config.getInt("block_distance") / 2;
        red_block_x = 8 + block_distance_from_center;
        blue_block_x = 8 - block_distance_from_center;

        int border_distance_from_block = config.getInt("border_distance");

        //Complicated calculation for Z coordinates for proper square play area.
        int play_area_z_min = blue_block_x - border_distance_from_block - 16;
        int play_area_z_max = red_block_x + border_distance_from_block + 16;


        this.getCommand("ll").setExecutor(new CommandKit());
        the_world = Bukkit.getWorld(world_name);

        //Creating the wall
        int timer = 30;
        for (int z = play_area_z_min; z <= play_area_z_max; z += 16) {
            Location spot = new Location(the_world, 8, 10, z);
            Chunk the_chunk = the_world.getChunkAt(spot);

            Block min_block = the_chunk.getBlock(7, 1, 0);
            Block max_block = the_chunk.getBlock(8, 255, 15);

            int x_min = min_block.getX();
            int y_min = min_block.getY();
            int z_min = min_block.getZ();

            int x_max = max_block.getX();
            int y_max = max_block.getY();
            int z_max = max_block.getZ();

            String fill_commnd = "minecraft:fill " + x_min + " " + y_min + " " + z_min + " " + x_max + " " + y_max + " " + z_max + " minecraft:bedrock 0";

            Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                @Override
                public void run() {
                    the_chunk.load();
                    run_as_console(fill_commnd);
                    the_chunk.unload(true);
                }
            }, timer);


            this.getLogger().info(Integer.toString(z) + " will be processed in " + Integer.toString(timer) + " ticks");
            timer += 10;
        }

        //Wall completion Message
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("Finished processing THE WALL");
            }
        }, timer);

        timer += 20;

        red_spawn_y = the_world.getHighestBlockYAt(red_block_x, 8) - 1;
        Location red_block_spot = new Location(the_world, red_block_x, red_spawn_y, 8);
        Chunk red_chunk = the_world.getChunkAt(red_block_spot);
        String red_block_command = "minecraft:setblock " + red_block_x + " " + red_spawn_y + " 8 minecraft:bedrock";

        blue_spawn_y = the_world.getHighestBlockYAt(blue_block_x, 8) - 1;
        Location blue_block_spot = new Location(the_world, blue_block_x, blue_spawn_y, 8);
        Chunk blue_chunk = the_world.getChunkAt(blue_block_spot);
        String blue_block_command = "minecraft:setblock " + blue_block_x + " " + blue_spawn_y + " 8 minecraft:bedrock";

        red_bedrock = the_world.getBlockAt(red_block_spot);
        blue_bedrock = the_world.getBlockAt(blue_block_spot);

        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run(){
                red_chunk.load();
                run_as_console(red_block_command);

                blue_chunk.load();
                run_as_console(blue_block_command);

                getLogger().info("DEFEND Blocks placed.");
            }
        }, timer);

        //Now, we will configure the teams
        red_team_str_list = config.getStringList("red");
        blue_team_str_list = config.getStringList("blue");

        timer += 20;

        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run(){
                //First, let's setup the teams

                //Create teams
                run_as_console("minecraft:scoreboard teams add red Red");
                run_as_console("minecraft:scoreboard teams add blue Blue");

                //Disable friendlyfire
                run_as_console("minecraft:scoreboard teams option red friendlyfire false");
                run_as_console("minecraft:scoreboard teams option blue friendlyfire false");

                //Change nametagVisibility to hideForOtherTeams
                run_as_console("minecraft:scoreboard teams option red nametagVisibility hideForOtherTeams");
                run_as_console("minecraft:scoreboard teams option blue nametagVisibility hideForOtherTeams");

                //Set team color
                run_as_console("minecraft:scoreboard teams option red color red");
                run_as_console("minecraft:scoreboard teams option blue color blue");
            }
        }, timer);

        timer += 5;

        //Add red players to their team
        for (String username : red_team_str_list){
            String red_team_command = "minecraft:scoreboard teams join red " + username;
            Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                @Override
                public void run() {
                    run_as_console(red_team_command);
                }
            }, timer);
        }

        timer += 5;

        //Add blue players to their team
        for (String username : blue_team_str_list){
            String blue_team_command = "minecraft:scoreboard teams join blue " + username;
            Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                @Override
                public void run() {
                    run_as_console(blue_team_command);
                }
            }, timer);
        }

        timer += 5;

        int border_size = border_distance_from_block * 2 + block_distance_from_center * 2;
        //Fundamental but simple preparation commands
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                run_as_console("minecraft:setworldspawn 8. 256. 8.");
                run_as_console("minecraft:defaultgamemode spectator");
                run_as_console("minecraft:gamerule doDaylightCycle false");
                run_as_console("minecraft:gamerule doWeatherCycle false");
                run_as_console("minecraft:gamerule spawnRadius 0");
                run_as_console("minecraft:time set 6000");

                //Border related stuff
                run_as_console("minecraft:worldborder center 8. 8.");
                run_as_console("minecraft:worldborder set " + border_size);
            }
        }, timer);

        timer += 10;

        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                getLogger().info("ALL GOOD TO JOIN!");
            }
        }, timer);
    }

    public class CommandKit implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (red_team_str_list.contains(player.getName())) {
                    Bukkit.dispatchCommand(sender, "lock 3 [r]");
                } else if (blue_team_str_list.contains(player.getName())) {
                    Bukkit.dispatchCommand(sender, "lock 3 [b]");
                } else {
                    sender.sendMessage("You are spectating. Command not available");
                }
            }
            return true;
        }
    }
}
