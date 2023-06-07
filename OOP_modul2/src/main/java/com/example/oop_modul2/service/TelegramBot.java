package com.example.oop_modul2.service;

import com.example.oop_modul2.config.BotConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;
    public TelegramBot(BotConfig config){
        this.config=config;
    }
    @Override
    public void onUpdateReceived(Update update) {

        if(update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();

            long chatId = update.getMessage().getChatId();


            String cityName = null;
            String firstPart = null;

            String[] parts =messageText.split(" ",2);

            if (parts.length == 1) {

                firstPart = parts[0];

            } else if (parts.length == 2) {

                firstPart = parts[0];

                cityName= parts[1];
            }



            if(messageText.equals("/start")){
                StartCommand(chatId, update.getMessage().getChat().getFirstName());
            }else if(firstPart.equals("/city")){
                cityCommand(chatId, cityName);
            }else{
                sendMessage(chatId,"Unknown command");
            }
        }

    }

    private void cityCommand(long chatId, String city){

        if(city==null){
            sendMessage(chatId,"Wrong city");
        }else {
            sendMessage(chatId, weather(city));

        }


    }

    private String weather(String name){
        String apiKey = "6FXdUYlqORtYicMJnNf7fNIv2n7scYJR";
        String weatherInfo = "";

        try {

            String encodedCityName = URLEncoder.encode(name, StandardCharsets.UTF_8);


            String searchUrl = "http://dataservice.accuweather.com/locations/v1/cities/search?apikey=" + apiKey + "&q=" + encodedCityName;
            HttpURLConnection searchConnection = (HttpURLConnection) new URL(searchUrl).openConnection();
            searchConnection.setRequestMethod("GET");


            int searchResponseCode = searchConnection.getResponseCode();
            if (searchResponseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader searchReader = new BufferedReader(new InputStreamReader(searchConnection.getInputStream()));
                StringBuilder searchResponse = new StringBuilder();
                String searchLine;
                while ((searchLine = searchReader.readLine()) != null) {
                    searchResponse.append(searchLine);
                }
                searchReader.close();


                JSONArray searchResults = new JSONArray(searchResponse.toString());
                if (searchResults.length() > 0) {
                    JSONObject cityObject = searchResults.getJSONObject(0);
                    String cityKey = cityObject.getString("Key");


                    String weatherUrl = "http://dataservice.accuweather.com/currentconditions/v1/" + cityKey + "?apikey=" + apiKey;
                    HttpURLConnection weatherConnection = (HttpURLConnection) new URL(weatherUrl).openConnection();
                    weatherConnection.setRequestMethod("GET");


                    int weatherResponseCode = weatherConnection.getResponseCode();
                    if (weatherResponseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader weatherReader = new BufferedReader(new InputStreamReader(weatherConnection.getInputStream()));
                        StringBuilder weatherResponse = new StringBuilder();
                        String weatherLine;
                        while ((weatherLine = weatherReader.readLine()) != null) {
                            weatherResponse.append(weatherLine);
                        }
                        weatherReader.close();


                        JSONArray weatherArray = new JSONArray(weatherResponse.toString());
                        if (weatherArray.length() > 0) {
                            JSONObject weatherObject = weatherArray.getJSONObject(0);
                            String weatherText = weatherObject.getString("WeatherText");
                            JSONObject temperatureObject = weatherObject.getJSONObject("Temperature").getJSONObject("Metric");
                            double temperature = temperatureObject.getDouble("Value");

                            weatherInfo += "Current weather in the city " + name + ":\n";
                            weatherInfo += "temperature: " + temperature + "°C\n";
                            weatherInfo += "Cloudiness: " + weatherText;
                        } else {
                            weatherInfo = "Failed to get weather information.";
                        }
                    } else {
                        weatherInfo = "Failed to get weather information. Error code: " + weatherResponseCode;
                    }
                } else {
                    weatherInfo = "Could not find city with name " + name;
                }
            } else {
                weatherInfo = "Failed to search city. Error code: " + searchResponseCode;
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return weatherInfo;
    }

    private void StartCommand(long chatId,String name){
        String ans = "Hi, " + name + ", choose your city. Write it like this \"/city your city\"";

        sendMessage(chatId,ans);
    }

    private void sendMessage(long chatId, String TextToSend){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(TextToSend);

        try{
            execute(message);
        }catch (TelegramApiException e){

        }
    }
    @Override
    public String getBotToken(){
        return config.getToken();
    }
    @Override
    public String getBotUsername() {
        return config.getBotName();
    }
}
