package org.example;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Answer_tracker extends ListenerAdapter {
    public static List<String> correct_answer = new ArrayList<>();
    public static Map<String, Integer> guesses = new HashMap<>();
    public static String answer = "";
    public static String channelId = "";

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        if (Emoji_guessing.isRunning) {
            if (e.getChannel().getId().equalsIgnoreCase(channelId)) {
                if(e.getAuthor().isBot()) return;
                String userId = e.getAuthor().getId();
                if(!guesses.containsKey(userId)){
                   guesses.put(userId, 1);
                }else{
                   guesses.put(userId, guesses.get(userId) + 1);
                }
                if(guesses.get(userId) >= 5){
                    if(guesses.get(userId) == 5){
                        e.getMessage().reply("**You've used your last guess!**").queue();
                    }
                    return;
                }
                if(e.getMessage().getContentRaw().equalsIgnoreCase(answer)){

                    if(!Emoji_guessing.tracker.containsKey( userId)){
                        Emoji_guessing.tracker.put(userId, 1);
                    }else if(!correct_answer.contains(userId)){
                        Emoji_guessing.tracker.put(userId, Emoji_guessing.tracker.get(userId)+ 1);
                    }

                    if(!correct_answer.contains(userId)){
                        correct_answer.add(userId);
                    }

                }
            }
        }
    }
}
