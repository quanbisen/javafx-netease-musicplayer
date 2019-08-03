package application;

import java.io.File;
import java.util.ArrayList;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import utils.XMLUtils;

public class ChooseFolderStage extends Stage{
	private double width=360;
	private double height=338;
	private boolean confirm;
	private ArrayList<CheckBox> checkboxList;
	Stage primaryStage;  //主舞台对象
	File file;
	public boolean isConfirm() {
		return confirm;
	}
	public void setConfirm(boolean confirm) {
		this.confirm = confirm;
	}
	
	private void initFolderList() {  //初始化并判断上次是否保存了选择的目录
		checkboxList = new ArrayList<>();
		file = new File("ChoseFolder.xml");
		if(file.exists()) {  //如果存储文件存在，读取之前保存的选择文件夹
			ArrayList<String> choseFolderList = (ArrayList<String>)XMLUtils.getAllRecord(file,"path");
			for(String folderPath:choseFolderList) {
				//创建CheckBox，并保存到List集合中
				CheckBox checkBox=new CheckBox(folderPath);
	    		checkBox.getStylesheets().add("css/CheckBoxStyle.css");
	    		checkBox.setFont(new Font("宋体",14));
	    		checkBox.setSelected(true);
	    		checkboxList.add(checkBox);
			}
		}
	}
	
	public ChooseFolderStage(Stage primaryStage) {
		this.initFolderList();
		this.primaryStage=primaryStage;
		this.initOwner(primaryStage);
		this.setX((primaryStage.getWidth()-width)/2.0+primaryStage.getX());
		this.setY((primaryStage.getHeight()-height)/2.0+primaryStage.getY());
		setSyncCenter(true);  //设置同步居中
		
		BorderPane borderPane=new BorderPane();
		borderPane.setPrefHeight(height);
		borderPane.setPrefWidth(width);
		borderPane.setBackground(new Background(new BackgroundFill(Color.WHITE,null,null)));
		borderPane.setBorder(new Border(new BorderStroke(Color.rgb(201, 201, 203),BorderStrokeStyle.SOLID, null,new BorderWidths(1))));
		
		Label labShowChooseFolderTips=new Label("选择本地音乐文件夹");
		labShowChooseFolderTips.setFont(new Font("宋体",16));
		labShowChooseFolderTips.setTextFill(Color.rgb(51, 51, 51));
		labShowChooseFolderTips.setPadding(new Insets(0,0,0,10));
		ImageView closeView=new ImageView("image/chooseFolderCloseIcon.png");
		closeView.setFitHeight(18);
		closeView.setFitWidth(19);
		Label labClose=new Label("",closeView);
		labClose.setOnMouseClicked(e->{ this.hide(); });
		labClose.setOnMouseEntered(e->{closeView.setImage(new Image("image/chooseFolderCloseIconHover.png"));});
		labClose.setOnMouseExited(e->{closeView.setImage(new Image("image/chooseFolderCloseIcon.png"));});
		HBox hBoxTop=new HBox(175);
		hBoxTop.setPrefHeight(40);
		hBoxTop.setAlignment(Pos.CENTER_LEFT);
		hBoxTop.getChildren().addAll(labShowChooseFolderTips,labClose);
		hBoxTop.setBorder(new Border(new BorderStroke(null, null, Color.rgb(232, 232, 234), null, null, null, BorderStrokeStyle.SOLID, null, null,new BorderWidths(1), null)));
		
		HBox hBoxInfo = new HBox();
		Label labInfo =new Label("将自动扫描您勾选的目录。");
		labInfo.setFont(new Font("宋体",12));
		labInfo.setTextFill(Color.rgb(153, 153, 153));
		labInfo.setPadding(new Insets(20,0,0,10));
		hBoxInfo.getChildren().add(labInfo);
		Button btnConfirm=new Button("确定");
        Button btnAddFolder=new Button("添加文件夹");
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
        	if(checkboxList!=null&&checkboxList.size()>=1) {  
        		if(!file.exists()) {  //如果文件不存在，创建文件
            		XMLUtils.createXML(file, "ChoseFolderList");
            	}
        		for(int index=0;index<checkboxList.size();index++) {
        			ArrayList<String> choseFolderList = (ArrayList<String>)XMLUtils.getAllRecord(file,"path");
        			if(checkboxList.get(index).isSelected()) {
        				if(!choseFolderList.contains(checkboxList.get(index).getText())) {
        					XMLUtils.addOneRecord(file,"ChoseFolder","path", checkboxList.get(index).getText());
        				}
        			}
        			else {
        				
        				if(choseFolderList.contains(checkboxList.get(index).getText())) {
        					XMLUtils.removeOneRecord(file, "path", checkboxList.get(index).getText());
        				}
        				checkboxList.remove(index);
    			        if(checkboxList.size()==0) {       //尝试删除存储文件
    		        		System.out.println("deleted ChoseFolderList.xml ...");
    		        		file.delete();
    			        }
        			}
        		}
        	}

        	this.setConfirm(true);
        	this.close();
        });
        ScrollPane scrollPane=new ScrollPane();
        
        VBox vWrapCheckBoxList=new VBox(20);  //装提示和选择的文件夹CheckBox
        vWrapCheckBoxList.getChildren().addAll(hBoxInfo);
        if(checkboxList!=null&&checkboxList.size()>0) {
        	for(CheckBox checkbox:checkboxList) {  //遍历从文件读取出来的checkbox，使用hbox容器，然后把这个hbox添加到vbox中
        		HBox hBoxCheckBox = new HBox();
            	hBoxCheckBox.setPadding(new Insets(0,0,0,10));
            	hBoxCheckBox.getChildren().add(checkbox);
        		vWrapCheckBoxList.getChildren().add(hBoxCheckBox);
            }
        }

        scrollPane.setContent(vWrapCheckBoxList);
        scrollPane.getStylesheets().add("css/ChooseFolderScrollPaneStyle.css");
        
        btnAddFolder.setPrefHeight(32);
        btnAddFolder.setPrefWidth(92);
        btnAddFolder.setPadding(new Insets(0));
        btnAddFolder.setFont(new Font("宋体",15));
        btnAddFolder.setBackground(new Background(new BackgroundFill(Color.rgb(255, 255, 255),null,null)));
        btnAddFolder.setTextFill(Color.rgb(200, 88, 86));
        btnAddFolder.setBorder(new Border(new BorderStroke(Color.rgb(222, 153, 153),BorderStrokeStyle.SOLID,null,new BorderWidths(1))));
        btnAddFolder.setOnMouseEntered(e->{
        	btnAddFolder.setBackground(new Background(new BackgroundFill(Color.rgb(254, 254, 254),null,null)));
        });
        btnAddFolder.setOnMouseExited(e->{
        	btnAddFolder.setBackground(new Background(new BackgroundFill(Color.rgb(255, 255, 255),null,null)));
        });
        btnAddFolder.setOnMouseClicked(e->{
        	DirectoryChooser directoryChooser = new DirectoryChooser();
        	File directory = directoryChooser.showDialog(this);
        	if(directory!=null) {
        		ArrayList<String> choseFolderList=new ArrayList<>();
        			String folderPath=directory.getPath();
        			if(file.exists()) {
        				choseFolderList = (ArrayList<String>)XMLUtils.getAllRecord(file,"path");
        			}
            		
    				if(!choseFolderList.contains(folderPath)) {
    					CheckBox checkBox=new CheckBox(folderPath);
    	        		checkBox.getStylesheets().add("css/CheckBoxStyle.css");
    	        		checkBox.setFont(new Font("宋体",14));
    	        		checkBox.setSelected(true);
    	        		
    	        		checkboxList.add(checkBox);

    	        		HBox hBoxCheckBox = new HBox();
    	            	hBoxCheckBox.setPadding(new Insets(0,0,0,10));
    	            	hBoxCheckBox.getChildren().add(checkBox);
    	            	
    	            	vWrapCheckBoxList.getChildren().add(hBoxCheckBox);
    				}
        		
        	}
        });
        HBox hBoxBottom=new HBox(12);
        hBoxBottom.setPrefHeight(64);
        hBoxBottom.setAlignment(Pos.CENTER);
        hBoxBottom.setBorder(new Border(new BorderStroke( Color.rgb(232, 232, 234), null, null,null, BorderStrokeStyle.SOLID, null, null, null, null,new BorderWidths(1), null)));
        hBoxBottom.getChildren().addAll(btnConfirm,btnAddFolder);
        
        borderPane.setTop(hBoxTop);
		borderPane.setCenter(scrollPane);
		borderPane.setBottom(hBoxBottom);
		Scene scene=new Scene(borderPane);
		this.setScene(scene);
		this.initStyle(StageStyle.UNDECORATED);
		this.initModality(Modality.NONE);
	}
	
	private void setSyncCenter(boolean bool) {  
		if(bool) {
			//主窗体坐标和宽度改变时需要触发的监听器，更新子窗体的位置，让子窗体一直居中显示
			primaryStage.xProperty().addListener(new ChangeListener<Number>() {

				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					ChooseFolderStage.this.setX((primaryStage.getWidth()-width)/2.0+newValue.doubleValue());
				}
				
			});
			primaryStage.yProperty().addListener(new ChangeListener<Number>() {

				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					ChooseFolderStage.this.setY((primaryStage.getHeight()-height)/2.0+newValue.doubleValue());
				}
				
			});
			primaryStage.widthProperty().addListener(new ChangeListener<Number>() {

				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					ChooseFolderStage.this.setX((newValue.doubleValue()-width)/2.0+primaryStage.getX());
				}
				
			});
			primaryStage.heightProperty().addListener(new ChangeListener<Number>() {

				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					ChooseFolderStage.this.setY((newValue.doubleValue()-height)/2.0+primaryStage.getY());
				}
				
			});
		}
	}
}
