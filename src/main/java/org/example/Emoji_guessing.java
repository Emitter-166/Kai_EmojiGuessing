package org.example;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.example.database.connection;

public class Emoji_guessing extends ListenerAdapter {
        static int loop_times = 0;
        public static boolean isRunning = false;
        static Map<String, Integer> tracker = new HashMap<>();
        Thread thread = null;
        @Override
        public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
                e.deferReply().queue();
                String name = e.getName();
                switch (name){
                        case "start-emoji-guessing":
                                if(!e.getMember().hasPermission(Permission.MODERATE_MEMBERS)){
                                        e.getHook().sendMessageEmbeds(new EmbedBuilder()
                                                .setTitle("You don't have permission to do that!")
                                                .build()).queue();
                                        return;
                                }
                                e.getHook().sendMessageEmbeds(new EmbedBuilder()
                                        .setColor(Color.green)
                                        .setTitle("Starting emoji guessing....") .build()).setEphemeral(true).queue(
                                                message -> message.delete().queue()
                                );
                                Runnable runnable = new Runnable() {
                                        @Override
                                        public void run() {
                                                try {
                                                        start(e.getChannel().getId(), e.getOption("times").getAsInt());
                                                } catch (SQLException | InterruptedException ex) {
                                                        throw new RuntimeException(ex);
                                                }
                                        }
                                };
                                 thread = new Thread(runnable);
                                thread.start();

                                break;
                        case "stop-emoji-guessing":
                                if(!e.getMember().hasPermission(Permission.MODERATE_MEMBERS)){
                                        e.getHook().sendMessageEmbeds(new EmbedBuilder()
                                                .setTitle("You don't have permission to do that!")
                                                .build()).queue();
                                        return;
                                }
                                e.getHook().sendMessageEmbeds(new EmbedBuilder()
                                                .setTitle("Cancelled current emoji guessing game!")
                                                .setColor(Color.white)
                                                .build()).queue();
                                sendLeaderboard();
                                de_initialize();
                                tracker = new HashMap<>();
                                Answer_tracker.correct_answer = new ArrayList<>();
                                thread.interrupt();
                                break;



                        case "delete-emoji":
                                if(!e.getMember().hasPermission(Permission.MODERATE_MEMBERS)){
                                        e.getHook().sendMessageEmbeds(new EmbedBuilder()
                                                .setTitle("You don't have permission to do that!")
                                                .build()).queue();
                                        return;
                                }
                                e.getHook().sendMessageEmbeds(new EmbedBuilder()
                                        .setTitle("Deleted an emoji!")
                                        .setColor(Color.white)
                                        .build()).queue();
                                int _id = e.getOption("_id").getAsInt();
                                String sql_delete = String.format("DELETE FROM emojis WHERE _id == %s", _id);
                                try {
                                        connection.createStatement().execute(sql_delete);
                                } catch (SQLException ex) {
                                        throw new RuntimeException(ex);
                                }
                                break;



                        case "add-emoji":
                                if(!e.getMember().hasPermission(Permission.MODERATE_MEMBERS)){
                                        e.getHook().sendMessageEmbeds(new EmbedBuilder()
                                                .setTitle("You don't have permission to do that!")
                                                .build()).queue();
                                        return;
                                }
                                String emoji = e.getOption("emojis").getAsString();
                                String answer = e.getOption("answer").getAsString();
                                String text = null;
                                try{
                                         text = e.getOption("text").getAsString();
                                }catch (NullPointerException ignored){}
                                if(text == null){
                                        text = "Guess the emoji!";
                                }
                                String sql = String.format("INSERT INTO emojis(emoji, answer, text_to_send) VALUES ('%s', '%s', '%s')", emoji, answer, text);
                                try {
                                        connection.createStatement().execute(sql);
                                        e.getHook().sendMessageEmbeds(new EmbedBuilder()
                                                .setColor(Color.green)
                                                .setTitle("Successfully added and emoji set!")
                                                .setDescription("```" + sql + "```")
                                                .build()).queue();

                                } catch (SQLException ex) {
                                     e.getHook().sendMessageEmbeds(new EmbedBuilder()
                                                .setColor(Color.red)
                                                .setTitle("An error occurred")
                                                .setDescription("```" + sql + "```")
                                                .build()).queue();
                                }

                                break;

                        case "show-emojis":
                                if(!e.getMember().hasPermission(Permission.MODERATE_MEMBERS)){
                                        e.getHook().sendMessageEmbeds(new EmbedBuilder()
                                                .setTitle("You don't have permission to do that!")
                                                .build()).queue();
                                        return;
                                }
                               File file = new File("Emojis.txt");
                                try {
                                        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                                        writer.write("_id        emoji        answer        text \n");
                                        writer.write("--------   ---------    ----------    --------- \n");
                                        ResultSet set = connection.createStatement().executeQuery("SELECT * FROM emojis");
                                        while(set.next()){
                                                writer.write(String.format("%s        %s        %s        %s \n",
                                                        set.getInt("_id"),
                                                        set.getString("emoji"),
                                                        set.getString("answer"),
                                                        set.getString("text_to_send")));
                                        }
                                        writer.close();
                                } catch (Exception ex) {
                                        throw new RuntimeException(ex);
                                }
                                e.getHook().sendMessage("").addFile(file, "emojis.txt").queue();
                                break;
                }
        }


        static void start(String channelId, int loop_times) throws SQLException, InterruptedException {
                initialize(channelId, loop_times);
                TextChannel channel = Main.jda.getTextChannelById(Answer_tracker.channelId);
                for(int i = 0; i < Emoji_guessing.loop_times; i++){

                        send();
                        Answer_tracker.correct_answer = new ArrayList<>();
                        Answer_tracker.guesses  = new HashMap<>();
                        if(!(i+1 == Emoji_guessing.loop_times)){
                                channel.sendMessage( Emoji_guessing.loop_times - (i+1) + " Questions remains this game!").queue();
                                TimeUnit.SECONDS.sleep(5);
                                channel.sendMessage("**15 seconds before next question** :stuck_out_tongue: :sunglasses: ").queue();
                                TimeUnit.SECONDS.sleep(15);
                        }
                }
                channel.sendMessage("**Game ended**").queue();
                de_initialize();
        }

        static void initialize(String channelId, int loop_times){
                Answer_tracker.channelId = channelId;
                isRunning = true;
                Emoji_guessing.loop_times = loop_times;
        }

        static void send() throws SQLException, InterruptedException {
                TextChannel channel = Main.jda.getTextChannelById(Answer_tracker.channelId);
                ResultSet set = connection.createStatement().executeQuery("SELECT * FROM emojis ORDER BY RANDOM() LIMIT 1");
                String text = set.getString("text_to_send");
                String question = set.getString("emoji");

                Answer_tracker.answer = set.getString("answer");
                if(text == null){
                        text = "**GUESS THE EMOJI ABOVE!!**";
                }
                MessageEmbed question_embed =new EmbedBuilder()
                        .setTitle(text)
                        .setDescription(question)
                        .setColor(Color.YELLOW)
                        .build();

                MessageEmbed timeRemaining = new EmbedBuilder()
                        .setTitle("20 SECONDS REMAINING")
                        .addField(text, question, false)
                        .setColor(Color.YELLOW)
                        .build();

                String[] answer_size = Answer_tracker.answer.split(" ");
                StringBuilder hint = new StringBuilder();
                if(answer_size.length == 1){
                        char[] chars = answer_size[0].toCharArray();
                        hint.append("**" + chars[0] + "** ");
                        for(int i = 1; i < chars.length; i++){
                                hint.append(" **_** ");
                        }
                }else{
                        hint.append("**" + answer_size[0] + "** ");
                        for(int i = 1; i < answer_size.length; i++){
                                for(int j = 0;  j < answer_size[i].length(); j++){
                                        hint.append(" **_** ");
                                }
                                hint.append("   ");
                        }
                }

                MessageEmbed hint_embed = new EmbedBuilder()
                        .setTitle("10 SECONDS REMAINING")
                        .addField("HINT", hint.toString(), false)
                        .setColor(Color.CYAN)
                        .build();
                MessageEmbed remaining_embed = new EmbedBuilder()
                        .setTitle("YOUR NEXT 5 MESSAGES WILL BE COUNTED AS GUESSES")
                        .setColor(Color.CYAN)
                        .build();
                channel.sendMessageEmbeds(question_embed).queue();
                channel.sendMessageEmbeds(remaining_embed).queue();
                TimeUnit.SECONDS.sleep(10);
                channel.sendMessageEmbeds(timeRemaining).queue();
                TimeUnit.SECONDS.sleep(10);
                channel.sendMessageEmbeds(hint_embed).queue();
                TimeUnit.SECONDS.sleep(10);

                sendResult();
                sendLeaderboard();
        }

        static void de_initialize(){
                Answer_tracker.channelId = "";
                isRunning = false;
                Emoji_guessing.loop_times = 0;
                tracker = new HashMap<>();
        }

        static void sendResult(){
                StringBuilder result = new StringBuilder();
                Answer_tracker.correct_answer.forEach(user -> {
                        result.append(String.format("<@%s> \n", user));
                });
                if(result.toString().equalsIgnoreCase("")){
                        result.append("`no one guessed the emoji in time!`");
                }
                TextChannel channel = Main.jda.getTextChannelById(Answer_tracker.channelId);
                channel.sendMessage(result.toString()).queue(
                        message -> message.delete().queue()
                );
                channel.sendMessageEmbeds(new EmbedBuilder()
                                .setTitle("Winners")
                                .setDescription(result)
                                .setColor(Color.WHITE)
                        .build()).queue();
        }

        static void sendLeaderboard(){
                StringBuilder winners = new StringBuilder();
                List<Map.Entry<String, Integer>> sorted = tracker.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).collect(Collectors.toList());
                for(int i = 0; i < sorted.size(); i++){
                        winners.append(String.format("`%s.` <@%s> **%s** points! \n", i+1, sorted.get(i).getKey(), sorted.get(i).getValue()));
                }
                Main.jda.getTextChannelById(Answer_tracker.channelId).sendMessageEmbeds(new EmbedBuilder()
                        .setTitle("points leaderboard")
                        .setDescription(winners)
                        .setColor(Color.WHITE)
                        .build()).queue();
        }

}
