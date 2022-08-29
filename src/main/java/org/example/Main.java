package org.example;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import javax.security.auth.login.LoginException;

public class Main {
    public static JDA jda;
    public static void main(String[] args) throws LoginException, InterruptedException {
        jda = JDABuilder.createLight(tokens.token)
                .addEventListeners(new Answer_tracker())
                .addEventListeners(new Emoji_guessing())
                .build().awaitReady();


        jda.getGuilds().forEach(guild -> {
            guild.upsertCommand("start-emoji-guessing", "starts an emoji guessing game!")
                    .addOption(OptionType.INTEGER, "times", "how many rounds you want to have", true).queue();
            guild.upsertCommand("stop-emoji-guessing", "stops emoji guessing game").queue();
            guild.upsertCommand("delete-emoji", "deletes an emoji set from emoji guessing game")
                    .addOption(OptionType.INTEGER, "_id", "provide emoji id from database", true).queue();
            guild.upsertCommand("add-emoji", "add an emoji guessing question to emoji guessing game")
                    .addOption(OptionType.STRING, "emojis", "the emojis you wanna add", true)
                    .addOption(OptionType.STRING, "answer", "correct answer of the emoji guessing", true)
                    .addOption(OptionType.STRING, "text", "questions you wanna add to the emoji guessing, by default its 'guess the emoji!`", false).queue();
            guild.upsertCommand("show-emojis", "returns a file with all the emojis").queue();
        });
    }
}