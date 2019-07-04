package edu.application;

import java.io.File;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import utils.XMLUtils;

public class AddGroupStage extends Stage{
	private double width=362;
	private double height=226;
	private boolean confirm;
	private String inputGroupName;
	Stage primaryStage;  //主舞台对象
	public String getInputGroupName() {
		return inputGroupName;
	}
	public boolean isConfirm() {
		return confirm;
	}
	public void setConfirm(boolean confirm) {
		this.confirm = confirm;
	}
	public AddGroupStage(Stage primaryStage){
		this.primaryStage=primaryStage;
		this.initOwner(primaryStage);
		this.initStyle(StageStyle.UNDECORATED);
		this.setX((primaryStage.getWidth()-width)/2.0+primaryStage.getX());
		this.setY((primaryStage.getHeight()-height)/2.0+primaryStage.getY());
		setSyncCenter(true);  //设置同步居中
		
		BorderPane borderPane=new BorderPane();
		borderPane.setPrefHeight(height);
		borderPane.setPrefWidth(width);
		borderPane.setBackground(new Background(new BackgroundFill(Color.WHITE,null,null)));
		borderPane.setBorder(new Border(new BorderStroke(Color.rgb(201, 201, 203),BorderStrokeStyle.SOLID, null,new BorderWidths(1))));
		Label labShowNewMusicGroup=new Label("新建歌单");
		labShowNewMusicGroup.setFont(new Font("宋体",16));
		labShowNewMusicGroup.setTextFill(Color.rgb(51, 51, 51));
		labShowNewMusicGroup.setPadding(new Insets(0,0,0,10));
		HBox hBoxTop=new HBox();
		hBoxTop.setPrefHeight(40);
		hBoxTop.setAlignment(Pos.CENTER_LEFT);
		hBoxTop.getChildren().add(labShowNewMusicGroup);
		hBoxTop.setBorder(new Border(new BorderStroke(null, null, Color.rgb(232, 232, 234), null, null, null, BorderStrokeStyle.SOLID, null, null,new BorderWidths(1), null)));
		
		TextField textInput = new TextField();
		textInput.setFocusTraversable(false);
		textInput.setPromptText("歌单标题");
		textInput.setFont(new Font("宋体",16));
		textInput.setPrefWidth(320);
		textInput.setPrefHeight(42);
		textInput.getStylesheets().add("css/TextFieldStyle.css");
        
        //红色提示歌单不能为空标签
        Label info=new Label();
        info.setTextFill(Color.rgb(188,47,45));
        info.setFont(new Font("宋体",12));
        info.setPadding(new Insets(0,0,0,25));
        
        
//        textInput.setOnKeyPressed(e->{
//        	info.setText("");
//        	if(textInput.getText().trim().equals("")) {
//    		info.setText("歌单名不能为空");
//    	}
//        });
        
        HBox hBoxGroupName=new HBox();
        hBoxGroupName.setAlignment(Pos.CENTER);
        hBoxGroupName.getChildren().addAll(textInput);
        HBox hBoxInfo=new HBox();
        hBoxInfo.setAlignment(Pos.CENTER_LEFT);
        hBoxInfo.getChildren().add(info);
        
        VBox vBoxCenter=new VBox(10);
        vBoxCenter.setAlignment(Pos.CENTER);
        vBoxCenter.getChildren().addAll(hBoxGroupName,hBoxInfo);
        
        
        Button btnConfirm=new Button("新建");
        Button btnCancel=new Button("取消");
        btnConfirm.setPrefHeight(32);
        btnConfirm.setPrefWidth(92);
        btnConfirm.setFont(new Font("宋体",15));
        btnConfirm.setBackground(new Background(new BackgroundFill(Color.rgb(188, 47, 45),null,null)));
        btnConfirm.setTextFill(Color.WHITE);
        btnConfirm.setOnMouseEntered(e->{
        	btnConfirm.setBackground(new Background(new BackgroundFill(Color.rgb(200, 88, 86),null,null)));
        });
        btnConfirm.setOnMouseExited(e->{
        	btnConfirm.setBackground(new Background(new BackgroundFill(Color.rgb(188, 47, 45),null,null)));
        });
        btnConfirm.setOnMouseClicked(e->{
        	String s=textInput.getText().trim();
        	if(!s.equals("")&&s.length()>0) {
        		
        		File file=new File("MusicGroup.xml");
        		if(!file.exists()) {  //XML文件不存在,创建
            		XMLUtils.createXML(file,"GroupList");
        		}
				List<String> groupNameList = XMLUtils.getAllRecord(file,"name");
				for (String groupName : groupNameList) {  //如果存在同名的歌单，关闭小舞台，返回不做处理
					if (groupName.equals(textInput.getText().trim())) {
						this.setConfirm(false);
		        		this.hide();
		        		return;
					}
				}
				inputGroupName=s;
        		XMLUtils.addOneRecord(file,"Group","name", s);
        		this.setConfirm(true);
        	}
        	this.close();
        });
        
        textInput.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				System.out.println("observable:"+observable.getValue());
				System.out.println("oldValue:"+oldValue);
				System.out.println("newValue:"+newValue);
				if(newValue.trim().equals("")) {
		    		info.setText("歌单名不能为空");
		    		btnConfirm.setMouseTransparent(true);
		    		btnConfirm.setBackground(new Background(new BackgroundFill(Color.rgb(228, 171, 171),null,null)));
				}
				else {
					info.setText("");
					btnConfirm.setMouseTransparent(false);
					btnConfirm.setBackground(new Background(new BackgroundFill(Color.rgb(188, 47, 45),null,null)));
				}
			}
        	
        });
        
        btnCancel.setPrefHeight(32);
        btnCancel.setPrefWidth(92);
        btnCancel.setFont(new Font("宋体",15));
        btnCancel.setBackground(new Background(new BackgroundFill(Color.rgb(255, 255, 255),null,null)));
        btnCancel.setTextFill(Color.rgb(200, 88, 86));
        btnCancel.setBorder(new Border(new BorderStroke(Color.rgb(222, 153, 153),BorderStrokeStyle.SOLID,null,new BorderWidths(1))));
        btnCancel.setOnMouseEntered(e->{
        	btnCancel.setBackground(new Background(new BackgroundFill(Color.rgb(254, 254, 254),null,null)));
        });
        btnCancel.setOnMouseExited(e->{
        	btnCancel.setBackground(new Background(new BackgroundFill(Color.rgb(255, 255, 255),null,null)));
        });
        btnCancel.setOnMouseClicked(e->{
        	this.hide();
        });
        HBox hBoxBottom=new HBox(12);
        hBoxBottom.setPrefHeight(64);
        hBoxBottom.setAlignment(Pos.CENTER_RIGHT);
        hBoxBottom.setPadding(new Insets(0,20,0,0));
        hBoxBottom.getChildren().addAll(btnConfirm,btnCancel);
        
        
		borderPane.setTop(hBoxTop);
		borderPane.setCenter(vBoxCenter);
		borderPane.setBottom(hBoxBottom);
		Scene scene=new Scene(borderPane);
		this.setScene(scene);
	}
	
	/**
	 * 设置是否同步居中在父窗体的函数
	  **/
	private void setSyncCenter(boolean bool) {  
		if(bool) {
			//主窗体坐标和宽度改变时需要触发的监听器，更新子窗体的位置，让子窗体一直居中显示
			primaryStage.xProperty().addListener(new ChangeListener<Number>() {

				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					AddGroupStage.this.setX((primaryStage.getWidth()-width)/2.0+newValue.doubleValue());
				}
				
			});
			primaryStage.yProperty().addListener(new ChangeListener<Number>() {

				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					AddGroupStage.this.setY((primaryStage.getHeight()-height)/2.0+newValue.doubleValue());
				}
				
			});
			primaryStage.widthProperty().addListener(new ChangeListener<Number>() {

				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					AddGroupStage.this.setX((newValue.doubleValue()-width)/2.0+primaryStage.getX());
				}
				
			});
			primaryStage.heightProperty().addListener(new ChangeListener<Number>() {

				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					AddGroupStage.this.setY((newValue.doubleValue()-height)/2.0+primaryStage.getY());
				}
				
			});
		}
	}
}
