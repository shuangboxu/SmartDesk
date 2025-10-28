package com.smartdesk.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Main application entry point for SmartDesk.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SmartDesk");

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(createTab("笔记", "笔记模块即将上线"));
        tabPane.getTabs().add(createTab("任务", "任务模块即将上线"));
        tabPane.getTabs().add(createTab("聊天", "聊天模块即将上线"));
        tabPane.getTabs().add(createTab("总结", "总结模块即将上线"));
        tabPane.getTabs().add(createTab("设置", "设置模块即将上线"));

        BorderPane root = new BorderPane(tabPane);
        Scene scene = new Scene(root, 960, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Tab createTab(String title, String placeholderText) {
        Tab tab = new Tab(title);
        tab.setContent(new Label(placeholderText));
        tab.setClosable(false);
        return tab;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
