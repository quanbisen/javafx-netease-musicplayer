package application;

import static com.sun.jna.platform.win32.WinUser.GWL_STYLE;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.FadeTransition;
import javafx.scene.control.*;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import utils.ResizeUtils;
import utils.XMLUtils;

public class Main extends Application {

	//舞台对象
	private Stage primaryStage;
	private double primaryStageMinWidth = 858;
	private double primaryStageMinHeight = 570;
	// 主舞台最底下的borderpane
	private BorderPane borderPane;
	//整个主舞台是一个StackPane
	private StackPane stageStackPane;
	
	//记录标题栏鼠标按下时的X，Y坐标，用做窗体移动的记录
	private double titleBarPressX;
	private double titleBarPressY;
	//记录tableSong按下的sceneX坐标，因为tableview组件的滑条阻挡了舞台边缘像素，临时解决办法。。。(见此类的getCenterPane()方法中tableSong设置的鼠标事件)
	private double mousePressedForResizeX;
	//下侧面板的显示歌曲名称和歌手
	private Label labMusicName;
	private Label labSinger;
	//下侧面板的已播放时间和总时间
	private Label labPlayedTime;
	private Label labTotalTime;
	//播放暂停的图片
	private ImageView playView;
	//音量滚动条和进度条
	private Slider volumeSlider;
	private ProgressBar volumeProgressBar;
	//控制播放时间的滑动条和进度条
	private Slider songSlider;
	private ProgressBar songProgressBar;
	//歌单名字集合
	private List<String> groupNameList;
	//主舞台左边的HBox标签集合
	private List<HBox> leftHBoxTagList;
	//歌曲表格的Item集合
	private ObservableList<SongInfo> songsInfo;
	//歌曲的表格容器
	private TableView<SongInfo> tableSong;
	//专辑的转动动画
	private Timeline timeline;
	//播放表格列表当前的索引
	private int currentPlayIndex;
	//播放表格列表上一首的索引列表集合
	private List<Integer> lastPlayIndexList;
	//播放表格列表下一首的索引列表集合
	private List<Integer> nextPlayIndexList;
	private String currentPlayMode = "随机播放";
	//播放器对象
	private Media media;
	private MediaPlayer mediaPlayer;
	// 程序最先执行这里
	@Override
	public void init() throws Exception {
		super.init();
		//初始化上一首和下一首的索引列表集合
		lastPlayIndexList = new ArrayList<>();
		nextPlayIndexList = new ArrayList<>();
		this.initGroupList();  //初始化已创建的歌单
		this.initSongList();   //初始化歌曲
	}

	private void initSongList() {
		File file = new File("ChoseFolder.xml");
		songsInfo = FXCollections.observableArrayList();
		if (file.exists()) { // 文件存在，读取
			songsInfo=this.getSongsInfo(file);
		}
	}

	private void initGroupList() {
		File file = new File("musicGroup.xml");
		groupNameList = new ArrayList<>();
		if (file.exists()) {
			groupNameList = XMLUtils.getAllRecord(file, "name");
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		//主舞台的最底下的borderPane（主界面）
		borderPane = new BorderPane();
		borderPane.setBackground(new Background(new BackgroundFill(Color.rgb(250, 250, 252), null, null)));
		borderPane.setTop(getTitleBarPane());
		borderPane.setLeft(getLeftPane());
		borderPane.setBottom(getBottomPane());
		borderPane.setCenter(getCenterPane());
		borderPane.setBorder(new Border(new BorderStroke(Color.rgb(110,110,111), BorderStrokeStyle.SOLID, null, new BorderWidths(1))));
		//整个主舞台的StackPane，（主界面）放在最底下，播放模式切换的提示信息可以动态的添加到stageStackPane的顶部，显示完成后也可以移除顶部的播放模式的提示信息
		stageStackPane = new StackPane();
		stageStackPane.getChildren().addAll(borderPane);

		Scene scene = new Scene(stageStackPane, primaryStageMinWidth, primaryStageMinHeight);
		primaryStage.setScene(scene);
		primaryStage.initStyle(StageStyle.UNDECORATED);
		primaryStage.show();
//		primaryStage.centerOnScreen();
		// 获取屏幕可视化的宽高（Except TaskBar），把窗体设置在可视化的区域居中
		primaryStage.setX((Screen.getPrimary().getVisualBounds().getWidth() - primaryStage.getWidth()) / 2.0);
		primaryStage.setY((Screen.getPrimary().getVisualBounds().getHeight() - primaryStage.getHeight()) / 2.0);
		primaryStage.setTitle("音乐");
		primaryStage.getIcons().add(new Image("image/NeteaseIcon.png"));// 设置任务栏图标
		ResizeUtils.addResizable(primaryStage,primaryStageMinWidth,primaryStageMinHeight);

		primaryStage.iconifiedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				//确保窗体在最大化状态下最小化后，单击任务栏图标显示时占据的屏幕大小是可视化的全屏
				if (primaryStage.isMaximized()){
					primaryStage.setHeight(Screen.getPrimary().getVisualBounds().getHeight());
					primaryStage.setWidth(Screen.getPrimary().getVisualBounds().getWidth());
				}
			}
		});

		/**
		 * 下面这段代码是使任务栏图标响应单击事件，当stage的initStyle设置成UNDECORATED时，任务栏图标单击无法最小化窗体
		 * 参见StackOverflow的提问：https://stackoverflow.com/questions/26972683/javafx-minimizing-undecorated-stage
		 * **/
		if (System.getProperties().getProperty("os.name").contains("Windows")) {  //判断当前os是否为Windows，如果是才执行
			long lhwnd = com.sun.glass.ui.Window.getWindows().get(0).getNativeWindow();
			Pointer lpVoid = new Pointer(lhwnd);
			WinDef.HWND hwnd = new WinDef.HWND(lpVoid);
			final User32 user32 = User32.INSTANCE;
			int oldStyle = user32.GetWindowLong(hwnd, GWL_STYLE);
//        System.out.println(Integer.toBinaryString(oldStyle));
			int newStyle = oldStyle | 0x00020000;//WS_MINIMIZEBOX
//        System.out.println(Integer.toBinaryString(newStyle));
			user32.SetWindowLong(hwnd, GWL_STYLE, newStyle);
//		user32.SetWindowLong(hwnd,0x00020000,0x00080000);
		}
	}

	public static void main(String[] args) {
		Application.launch(args);  //启动程序
	}

	//创建上侧的最小化、最大化和退出的标题栏面板
	private BorderPane getTitleBarPane() {
		Label title=new Label("音乐");
		title.setTextFill(Color.WHITE);
		title.setFont(new Font("宋体",13));
		title.prefHeight(28);
		
		HBox leftHBox=new HBox();
		leftHBox.setPrefWidth(600);
		leftHBox.setAlignment(Pos.CENTER_LEFT);
		leftHBox.setPadding(new Insets(0,0,0,20));
		
		leftHBox.getChildren().add(title);
		
		ImageView minimizeView=new ImageView("image/NeteaseMinimizeDefault.png");
		minimizeView.setFitHeight(32);
		minimizeView.setFitWidth(46);
		Label labMinimize=new Label("",minimizeView);
		labMinimize.setOnMouseEntered(e -> {
			if(primaryStage.getScene().getCursor()==Cursor.DEFAULT) {
				minimizeView.setImage(new Image("image/NeteaseMinimize.png"));
			}
		});
		labMinimize.setOnMouseExited(e -> {
			minimizeView.setImage(new Image("image/NeteaseMinimizeDefault.png"));
		});
		labMinimize.setOnMouseMoved(e->{
			if(primaryStage.getScene().getCursor()==Cursor.DEFAULT){
				minimizeView.setImage(new Image("image/NeteaseMinimize.png"));
			}
			else{
				minimizeView.setImage(new Image("image/NeteaseMinimizeDefault.png"));
			}
		});
		labMinimize.setOnMouseClicked(e -> {
			if(primaryStage.getScene().getCursor()==Cursor.DEFAULT){
				this.primaryStage.setIconified(true); // 设置最小化
			}
		});
		
		ImageView MaximizeView=new ImageView("image/NeteaseMaximizeDefault.png");
		MaximizeView.setFitHeight(32);
		MaximizeView.setFitWidth(46);
		Label labMaximize=new Label("",MaximizeView);
		labMaximize.setOnMouseEntered(e -> {
			if(primaryStage.getScene().getCursor()==Cursor.DEFAULT) {
				MaximizeView.setImage(new Image("image/NeteaseMaximize.png"));
			}
		});
		labMaximize.setOnMouseExited(e -> {
			MaximizeView.setImage(new Image("image/NeteaseMaximizeDefault.png"));
		});
		labMaximize.setOnMouseMoved(e->{
			if(primaryStage.getScene().getCursor()==Cursor.DEFAULT){
				MaximizeView.setImage(new Image("image/NeteaseMaximize.png"));
			}
			else{
				MaximizeView.setImage(new Image("image/NeteaseMaximizeDefault.png"));
			}
		});
		labMaximize.setOnMouseClicked(e->{
			if(primaryStage.getScene().getCursor()==Cursor.DEFAULT){
				if (!primaryStage.isMaximized()){  //如果不是最大化，设置最大化
					ResizeUtils.setMaximized(true,primaryStage,labMaximize,MaximizeView);
				}
				else{  //状态是最大化时执行恢复原来的状态
					ResizeUtils.setMaximized(false,primaryStage,labMaximize,MaximizeView);
				}
			}
		});
		
		ImageView exitView=new ImageView("image/NeteaseExitDefault.png");
		exitView.setFitHeight(32);
		exitView.setFitWidth(46);
		Label labExit=new Label("",exitView);
		labExit.setOnMouseEntered(e->{

		});
		labExit.setOnMouseEntered(e -> {
			if(primaryStage.getScene().getCursor()==Cursor.DEFAULT) {
				exitView.setImage(new Image("image/NeteaseExit.png"));
			}
		});
		labExit.setOnMouseExited(e -> {
			exitView.setImage(new Image("image/NeteaseExitDefault.png"));
		});
		labExit.setOnMouseMoved(e->{
			if(primaryStage.getScene().getCursor()==Cursor.DEFAULT){
				exitView.setImage(new Image("image/NeteaseExit.png"));
			}
			else{
				exitView.setImage(new Image("image/NeteaseExitDefault.png"));
			}
		});
		labExit.setOnMouseClicked(e->{
			if (primaryStage.getScene().getCursor()==Cursor.DEFAULT){
				primaryStage.close();
			}
		});
		
		HBox rightHBox=new HBox(0);
		rightHBox.setAlignment(Pos.CENTER);
		rightHBox.setPrefWidth(100);
		rightHBox.getChildren().addAll(labMinimize,labMaximize,labExit);
		
		//创建BorderPane容器包裹标题文字和最大化，最小化按钮，退出按钮
		BorderPane titleBarPane=new BorderPane();
		titleBarPane.setLeft(leftHBox);
		titleBarPane.setRight(rightHBox);
		titleBarPane.setBackground(new Background(new BackgroundFill(Color.rgb(188, 47, 46), null, null)));
		// 为titleBarBorderPane添加按下和拖拽事件，实现移动窗体功能
		titleBarPane.setOnMousePressed(e -> {
			if(!labMinimize.isPressed()&&!labMaximize.isPressed()&&!labExit.isPressed()) {
				this.titleBarPressX = e.getX();
				this.titleBarPressY = e.getY();
			}
		});
		titleBarPane.setOnMouseDragged(e -> {
			if(primaryStage.getScene().getCursor()==Cursor.DEFAULT){
				if(!labMinimize.isPressed()&&!labMaximize.isPressed()&&!labExit.isPressed()) {
					//屏幕的位置在可视化的区域内才进行移动(任务栏不是可视化区域)
					if(e.getScreenY()<=Screen.getPrimary().getVisualBounds().getHeight()){
						if(this.primaryStage.isMaximized()){  //如果是最大化状态下拖拽，变为未最大化的状态
							//记录计算按下鼠标时的百分比(注意Y坐标不需要计算，因为Y本身没有变化)
							double percentageX=titleBarPressX/this.primaryStage.getWidth();
							//设置成未最大化的状态
							ResizeUtils.setMaximized(false,primaryStage,labMaximize,MaximizeView);
							//重新计算未最大化的状态的鼠标按下坐标
							titleBarPressX = this.primaryStage.getWidth()*percentageX;
							//更新主舞台的坐标
							this.primaryStage.setX(e.getScreenX() - titleBarPressX);
							this.primaryStage.setY(e.getScreenY() - titleBarPressY );
						}
						else{  //否则为最大化状态，直接更新主舞台的坐标
							this.primaryStage.setX(e.getScreenX() - this.titleBarPressX);
							this.primaryStage.setY(e.getScreenY() - this.titleBarPressY);
						}
					}
				}
			}
		});
		//为titleBarBorderPane添加双击事件，实现双击标题栏最大化、最小化
		titleBarPane.setOnMouseClicked(e->{
			if(primaryStage.getScene().getCursor()==Cursor.DEFAULT){
				if(e.getButton()==MouseButton.PRIMARY) {  //鼠标左键
					if(e.getClickCount()==2) {  //双击
						if (!primaryStage.isMaximized()){  //如果不是最大化，设置最大化
							ResizeUtils.setMaximized(true,primaryStage,labMaximize,MaximizeView);
						}
						else{  //状态是最大化时执行恢复原来的状态
							ResizeUtils.setMaximized(false,primaryStage,labMaximize,MaximizeView);
						}
					}
				}
			}
		});
		return titleBarPane;
	}

	// 创建左侧显示信息和歌单的面板
	private BorderPane getLeftPane() {
		// 1.史迪仔图片
		ImageView imgView = new ImageView("image/Stitch.jpg");
		imgView.setFitWidth(180);
		imgView.setPreserveRatio(true);// 设置图片保持宽高比
		
		Circle stitchCircle = new Circle();//设置圆形
		stitchCircle.setCenterX(90);
		stitchCircle.setCenterY(90);
		stitchCircle.setRadius(90);
		imgView.setClip(stitchCircle);

		// 2.作者label
		Label labAuthor = new Label("Author: Super Lollipop");
		labAuthor.setPrefWidth(180);
		labAuthor.setTextFill(Color.rgb(153, 153, 153));
		labAuthor.setFont(new Font("黑体", 16));
		labAuthor.setAlignment(Pos.CENTER);

		// 3.制作日期
		Label labDate = new Label("日期：2019-03-18");
		labDate.setPrefWidth(180);
		labDate.setTextFill(Color.rgb(153, 153, 153));
		labDate.setFont(new Font("黑体", 16));
		labDate.setAlignment(Pos.CENTER);

		// 4.我的音乐label
		Label labMyMusicShow = new Label("我的音乐");
		labMyMusicShow.setPrefWidth(75);
		labMyMusicShow.setFont(new Font("黑体", 12));
		labMyMusicShow.setPadding(new Insets(20, 0, 10, 4));
		labMyMusicShow.getStyleClass().add("infoLabel");

		leftHBoxTagList = new ArrayList<>();
		// 5.本地音乐HBox
		// 本地音乐的图片
		ImageView localMusicView = new ImageView("image/LocalMusicIcon.png");
		Label labLocalMusicIcon = new Label("", localMusicView);
		HBox localMusicTag = this.createLeftHBoxTag(labLocalMusicIcon,"本地音乐");

		// 6.最近播放音乐HBox
		ImageView recentPlayMusicView = new ImageView("image/RecentPlayIcon.png");
		Label labRecentPlayMusicIcon = new Label("", recentPlayMusicView);
		HBox recentPlayTag = this.createLeftHBoxTag(labRecentPlayMusicIcon,"最近播放");

		// 7.添加歌单
		// 7.1显示"创建的歌单"的label
		Label labCreatedMusicGroup = new Label("创建的歌单");
		labCreatedMusicGroup.setPrefWidth(75);
		labCreatedMusicGroup.setFont(new Font("宋体", 12));
		labCreatedMusicGroup.setPadding(new Insets(5, 0, 0, 4));
		labCreatedMusicGroup.getStyleClass().add("infoLabel");
		// 7.2显示添加歌单的imageView
		ImageView addMusicGroupView = new ImageView("image/NeteaseAddMusicGroup.png");
		addMusicGroupView.setFitWidth(20);
		addMusicGroupView.setFitHeight(20);
		Label addMusicGroupIcon = new Label("", addMusicGroupView);
		addMusicGroupIcon.setPadding(new Insets(2, 0, 0, 0));

		// 一个HBox容器包裹“创建的歌单”label和ImageLabel
		HBox hBox = new HBox(82);
		hBox.setPrefHeight(40);
		hBox.setAlignment(Pos.CENTER_LEFT);
		hBox.getChildren().addAll(labCreatedMusicGroup, addMusicGroupIcon);

		// 一个VBox容器包裹logo，作者，制作日期，HBox（label和ImageLabel）
		VBox vBox = new VBox();
		vBox.setPadding(new Insets(5, 5, 5, 0));
		vBox.getChildren().addAll(imgView, labAuthor, labDate, labMyMusicShow, localMusicTag, recentPlayTag, hBox);
		if (groupNameList != null&&groupNameList.size()>0) {
			for (String groupName:groupNameList){
				ImageView image = new ImageView("image/MusicGroupListIcon.png");
				Label labelIcon = new Label("", image);
				HBox leftTag = this.createLeftHBoxTag(labelIcon,groupName);
				vBox.getChildren().add(leftTag);
			}
		}

		// 设置鼠标事件
		addMusicGroupIcon.setOnMouseClicked(e -> {
			if(e.getButton()==MouseButton.SECONDARY) {

				GlobalMenu globalMenu = GlobalMenu.getInstance();
				addMusicGroupIcon.setContextMenu(globalMenu);
				globalMenu.show(addMusicGroupIcon, Side.RIGHT, 0, 0);
			}
			if (e.getButton() == MouseButton.PRIMARY) {
				//调用阻止主界面响应鼠标事件的函数
				this.blockBorderPane();
				AddGroupStage addGroupStage = new AddGroupStage(primaryStage);
				addGroupStage.showAndWait();
				if (addGroupStage.isConfirm()) { // 判断确定按钮是否成功按下，成功后读取xml文件中的歌单
					ImageView image = new ImageView("image/MusicGroupListIcon.png");
					Label labelIcon = new Label("", image);
					HBox leftTag = this.createLeftHBoxTag(labelIcon,addGroupStage.getInputGroupName());
					vBox.getChildren().add(leftTag);
				}
				//调用释放主界面响应鼠标事件的函数
				this.releaseBorderPane();
			}
		});

		Pane content = new Pane();
		content.getChildren().addAll(vBox);

		ScrollPane scrollpane = new ScrollPane();
		scrollpane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollpane.setPadding(new Insets(0,5,0,0));
		scrollpane.setContent(content);
		scrollpane.setBorder(new Border(new BorderStroke(null,Color.rgb(221, 221, 225), Color.rgb(221, 221, 225),  null, null,BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
				 null, null, new BorderWidths(0,1,0,0), null)));
		scrollpane.getStylesheets().add("css/ScrollPane.css");

		HBox hBoxUserInfo = new HBox(10);

		hBoxUserInfo.setMinHeight(50);
		hBoxUserInfo.setAlignment(Pos.CENTER_LEFT);
		ImageView userIcon = new ImageView("image/Stitch32.jpg");
		userIcon.setFitWidth(32);
		userIcon.setPreserveRatio(true);// 设置图片保持宽高比
		// 设置图片为圆形
		Circle userIconCircle = new Circle();
		userIconCircle.setCenterX(16);
		userIconCircle.setCenterY(16);
		userIconCircle.setRadius(16);
		userIcon.setClip(userIconCircle);
		Label userName = new Label("Lollipop");
		hBoxUserInfo.getChildren().addAll(userIcon, userName);
		hBoxUserInfo.setPadding(new Insets(0, 0, 0, 10));
		hBoxUserInfo.setBorder(new Border(new BorderStroke(Color.rgb(223, 223, 245), Color.rgb(223, 223, 245), null, null, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, null, null, null, new BorderWidths(1,1,0,0), null)));

		BorderPane leftPane = new BorderPane();
		leftPane.setBackground(new Background(new BackgroundFill(Color.rgb(243, 243, 245), null, null)));
		leftPane.setPadding(new Insets(0,0,0,5));

		leftPane.setCenter(scrollpane);
		leftPane.setBottom(hBoxUserInfo);
		return leftPane;
	}

	private BorderPane getCenterPane() {
		// 定义一个圆
		Circle circle = new Circle();
		circle.setCenterX(75);
		circle.setCenterY(75);
		circle.setRadius(75);// 圆的半径
		ImageView albumView = new ImageView("image/NeteaseDefaultAlbum.png");
		albumView.setFitHeight(150);
		albumView.setFitWidth(150);
		albumView.setClip(circle);

		Label labAlbum = new Label("", albumView);
		labAlbum.setLayoutX(25);
		labAlbum.setLayoutY(25);
		// 定义一个"时间轴"动画
		timeline = new Timeline();
		timeline.getKeyFrames().addAll(
				new KeyFrame(new Duration(1000*10), new KeyValue(albumView.rotateProperty(), 360))
				);
		timeline.setCycleCount(Timeline.INDEFINITE);// 无限循环

//		timeline.play();

		// 显示歌词VBox
		VBox lyricVBox = new VBox(15);
		lyricVBox.setPadding(new Insets(20, 20, 20, 20));
		lyricVBox.setLayoutX(250);
		lyricVBox.setLayoutY(0);
		lyricVBox.setPrefHeight(200);
		lyricVBox.setPrefWidth(200);
		lyricVBox.setBorder(
				new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(1))));
		// 模糊背景
		Image image = new Image("image/NeteaseDefaultAlbum.png");
		PixelReader pixelReader = image.getPixelReader();
		WritableImage writableImage = new WritableImage((int) image.getWidth(), (int) image.getHeight());
		PixelWriter pixelWriter = writableImage.getPixelWriter();
		for (int i = 0; i < image.getWidth(); i++)
			for (int j = 0; j < image.getHeight(); j++) {
				Color pixelColor = pixelReader.getColor(i, j);
				for (int n = 0; n < 4; n++) { // 四次颜色淡化
					pixelColor = pixelColor.darker();
				}
				pixelWriter.setColor(i, j, pixelColor);
			}
		ImageView ablumBackground = new ImageView(writableImage);
		ablumBackground.setX(0);
		ablumBackground.setY(0);
		GaussianBlur gasussian = new GaussianBlur();
		gasussian.setRadius(20);
		ablumBackground.setEffect(gasussian);

		AnchorPane anchorpane = new AnchorPane();
		anchorpane.getChildren().addAll(ablumBackground, labAlbum, lyricVBox);

		ScrollPane scrollpane = new ScrollPane();
		scrollpane.setContent(anchorpane);
		scrollpane.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
		scrollpane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollpane.setVbarPolicy(ScrollBarPolicy.NEVER);
		scrollpane.setPrefHeight(200);
		scrollpane.setPadding(new Insets(0, 10, 0, 10));
		ablumBackground.fitWidthProperty().bind(scrollpane.widthProperty());
		ablumBackground.fitHeightProperty().bind(scrollpane.heightProperty());

		TableColumn<SongInfo, String> musicName = new TableColumn<>("歌曲名称");
		musicName.setPrefWidth(200);
		musicName.setMaxWidth(200);
		musicName.setCellValueFactory(new PropertyValueFactory<>("musicName"));
		musicName.getStyleClass().add("musicName"); // 给这一列添加类名字
		musicName.setSortType(SortType.ASCENDING);

		TableColumn<SongInfo, String> singer = new TableColumn<>("歌手");
		singer.setPrefWidth(100);
		singer.setMaxWidth(100);
		singer.setCellValueFactory(new PropertyValueFactory<>("singer"));

		TableColumn<SongInfo, String> album = new TableColumn<>("专辑");
		album.setPrefWidth(90);
		album.setMaxWidth(90);
		album.setCellValueFactory(new PropertyValueFactory<>("album"));

		TableColumn<SongInfo, String> totalTime = new TableColumn<>("时长");
		totalTime.setPrefWidth(70);
		totalTime.setMaxWidth(70);
		totalTime.setCellValueFactory(new PropertyValueFactory<>("totalTime"));

		TableColumn<SongInfo, String> size = new TableColumn<>("大小");
		size.setPrefWidth(80);
		size.setMaxWidth(80);
		size.setMinWidth(80);
		size.setCellValueFactory(new PropertyValueFactory<>("size"));

		tableSong = new TableView<>();
		tableSong.getColumns().add(musicName);
		tableSong.getColumns().add(singer);
		tableSong.getColumns().add(album);
		tableSong.getColumns().add(totalTime);
		tableSong.getColumns().add(size);
		tableSong.getStylesheets().add("css/TableViewStyle.css");
		//因为TableView右侧的ScrollBar阻挡了borderPane的边缘像素，不得不空出一部分像素用做自由缩放，这里设置了3个像素，见TableViewStyle.css样式的padding
		tableSong.setOnMouseMoved(e->{
			if (primaryStage.getWidth() - e.getSceneX() <= 2 ) {
				tableSong.setCursor(Cursor.E_RESIZE);
			}
			else {
				tableSong.setCursor(Cursor.DEFAULT);
			}
		});
		tableSong.setOnMousePressed(e->{
			mousePressedForResizeX=e.getSceneX();
		});
		tableSong.setOnMouseDragged(e->{
			if (primaryStage.getWidth() + (e.getSceneX() - mousePressedForResizeX) >= primaryStageMinWidth
					&& e.getScreenX() < 1920-2) {
				primaryStage.setWidth(primaryStage.getWidth() + (e.getSceneX() - mousePressedForResizeX));
				mousePressedForResizeX = e.getSceneX();
			}
		});

		// 如果读取到的歌曲songs不为空，设置表格的内容
		if (songsInfo != null && songsInfo.size() > 0) {
			tableSong.setItems(songsInfo);
			tableSong.getSortOrder().add(musicName);
			tableSong.sort();
		}
		tableSong.columnResizePolicyProperty().setValue(TableView.CONSTRAINED_RESIZE_POLICY);
		tableSong.setRowFactory(tv -> {
			TableRow<SongInfo> row = new TableRow<>();
			row.setOnMouseClicked(e -> {
				//鼠标左键双击执行播放
				if (e.getClickCount() == 2&&e.getButton()==MouseButton.PRIMARY) {
					if(mediaPlayer==null) {   //是首次播放吗？
						//是的话，就记录这一行表格的索引，然后播放
						this.currentPlayIndex=row.getIndex();
						this.mediaAutoPlay(row.getItem());
					}
					else{   //否则再次判断是否是正在播放的这首歌，如果不是才执行新的播放
						if(currentPlayIndex!=row.getIndex()){
							//如果当前播放模式是“随机播放”，清空用于记录随机播放上一首下一首的列表
							if(currentPlayMode.equals("随机播放")){
								nextPlayIndexList.clear();
								lastPlayIndexList.clear();
							}
							this.currentPlayIndex=row.getIndex();
							this.mediaDestroy(); //释放之前的播放器资源
							this.mediaAutoPlay(row.getItem());
						}
					}
				}
			});
			//表格的鼠标进入、退出事件
			row.setOnMouseEntered(e -> {
				if (!row.isSelected()) {
					row.setBackground(new Background(new BackgroundFill(Color.rgb(242, 242, 243), null, null)));
				}
				if (row.isSelected()) {
					row.setBackground(new Background(new BackgroundFill(Color.rgb(222, 222, 224), null, null)));
				}
			});
			row.setOnMouseExited(e -> {
				if (row.getIndex() % 2 == 0) {
					row.setBackground(new Background(new BackgroundFill(Color.rgb(250, 250, 252), null, null)));
				} else {
					row.setBackground(new Background(new BackgroundFill(Color.rgb(244, 244, 246), null, null)));
				}
				if (row.isSelected()) {
					row.setBackground(new Background(new BackgroundFill(Color.rgb(222, 222, 224), null, null)));
				}
			});
			return row;
		});

		// "选择目录"
		BorderPane borderPaneChooseFolder = new BorderPane();
		Label lab = new Label("本地音乐");
		lab.setAlignment(Pos.CENTER);
		lab.setPrefHeight(30);
		lab.setTextFill(Color.rgb(102, 102, 102));
		lab.setPadding(new Insets(20, 0, 0, 25));
		ImageView chooseFolderIcon = new ImageView("image/ChooseFolderIcon.png");

		Label choose = new Label("选择目录");
		choose.setTextFill(Color.rgb(26, 90, 153));
		HBox h = new HBox();
		h.setAlignment(Pos.CENTER);
		h.getChildren().addAll(chooseFolderIcon, choose);
		h.setPadding(new Insets(20, 25, 0, 0));
		//“选择目录”的监听事件
		h.setOnMouseClicked(e -> {
			//调用阻止主界面响应鼠标事件的函数
			this.blockBorderPane();
			ChooseFolderStage chooseFolderStage = new ChooseFolderStage(primaryStage);
			chooseFolderStage.showAndWait();
			//确定按钮按下时读取ChoseFolder.xml文件保存的路径，扫描路径下面的音乐文件
			if (chooseFolderStage.isConfirm()) {
				//首先需要处理mediaPlayer播放器对象，释放资源
				if (mediaPlayer != null) {
					this.mediaDestroy();
					//设置歌名歌歌手为未知，播放暂停的按钮为暂停按钮
					labMusicName.setText("未知");
					labSinger.setText("未知");
					this.playView.setImage(new Image("image/NeteasePause.png"));
				}
				//如果歌曲集合的大小大于0，清除集合
				if (songsInfo.size() > 0) {
					songsInfo.clear();
				}
				File file = new File("ChoseFolder.xml");
				if (file.exists()) {  //文件存在，读取文件里面的歌曲信息
					songsInfo=this.getSongsInfo(file);
					}
				//设置歌曲表格的内容并按歌名这列排序
				tableSong.setItems(songsInfo);
				tableSong.getSortOrder().add(musicName);
				tableSong.sort();
			}
			//调用释放主界面响应鼠标事件的函数
			this.releaseBorderPane();
		});

		borderPaneChooseFolder.setLeft(lab);
		borderPaneChooseFolder.setPrefHeight(30);
		borderPaneChooseFolder.setRight(h);


		BorderPane borderPaneCenter = new BorderPane();
		borderPaneCenter.setTop(borderPaneChooseFolder);
		borderPaneCenter.setCenter(tableSong);
		borderPaneCenter.setPadding(new Insets(0));

		BorderPane borderpane = new BorderPane();
//		borderpane.setTop(scrollpane);
		borderpane.setCenter(borderPaneCenter);
		return borderpane;
	}

	// 创建下侧的播放控制面板，包括上一首、暂停、下一首，播放时间显示，进度条显示等
	private BorderPane getBottomPane() {

		//专辑图片
		ImageView album = new ImageView("image/NeteaseDefaultAlbumWhiteBackground.png");

		album.setFitHeight(58);
		album.setFitWidth(58);
		Label labAlbum = new Label("",album);
		labAlbum.setBorder(new Border(new BorderStroke(Color.rgb(242, 242, 242),BorderStrokeStyle.SOLID,null,new BorderWidths(1))));
		labAlbum.getStylesheets().add("css/LabelScaleStyle.css");
		labAlbum.setOnMouseClicked(e->{
			Pane pane = new Pane(new Label("Just a test."));
			pane.setPrefWidth(128);
			pane.setPrefHeight(128);
			pane.setBackground(new Background(new BackgroundFill(Color.rgb(221,221,221),null,null)));
			if (e.getButton()==MouseButton.PRIMARY){
				borderPane.setLeft(null);
				borderPane.setCenter(pane);
				FadeTransition fadeTransition = new FadeTransition(Duration.seconds(2.5),pane);
				fadeTransition.setFromValue(0);
				fadeTransition.setToValue(1);
				//开始播放渐变动画提示
				fadeTransition.play();
			}
			else if (e.getButton()==MouseButton.SECONDARY){
				borderPane.setLeft(this.getLeftPane());
				borderPane.setCenter(this.getCenterPane());
			}

		});
		// 上一首label
		ImageView lastView = new ImageView("image/NeteaseLast.png");
		lastView.setFitHeight(30);
		lastView.setFitWidth(30);
		Label labPlayLast = new Label("", lastView);
		labPlayLast.setPrefWidth(30);
		labPlayLast.setPrefHeight(30);
		labPlayLast.getStylesheets().add("css/LabelScaleStyle.css");
//		labPlayLast.setPadding(new Insets(10,10,10,20));
		// 为上一首label添加鼠标事件
		labPlayLast.setOnMouseClicked(e->{
			if(mediaPlayer!=null){
				//表格的歌曲只有一首歌时执行的处理
				if (tableSong.getItems().size()==1){
					//重新播放第一首歌，即原来的这首歌
					currentPlayIndex=0;
					this.mediaAutoPlay(tableSong.getItems().get(currentPlayIndex));
				}
				//否则表格的歌曲大于1，播放上一首歌曲
				else{
					//如果当前播放模式是“随机播放”,随机生成一个非当前正在的播放的索引值执行播放
					if(this.currentPlayMode.equals("随机播放")){
						nextPlayIndexList.add(currentPlayIndex);  //播放上一首歌曲之前，把当前的索引添加到下一次播放的索引列表
						//如果记录上一首播放的歌曲的列表小于0，证明当前没有上一首歌播放，根据随机播放模式，随机生成一个索引值播放
						if (lastPlayIndexList.size()==0){
							//直到生成的随机数不是当前播放的索引值，执行播放
							while (true){
								int randomIndex=new Random().nextInt(tableSong.getItems().size());
								if (randomIndex!=currentPlayIndex){
									currentPlayIndex=randomIndex;
									break;
								}
							}
							this.mediaAutoPlay(tableSong.getItems().get(currentPlayIndex));
						}
						//否则，播放记录上一首歌列表里的最后一次添加的那一个
						else{
							int index = lastPlayIndexList.size()-1;
							this.mediaAutoPlay(tableSong.getItems().get(lastPlayIndexList.get(index)));
							currentPlayIndex = lastPlayIndexList.get(index);
							lastPlayIndexList.remove(index);
						}
					}
					//否则其它的模式在表格歌曲大于1时都是执行当前的索引值-1播放
					else {
						currentPlayIndex=currentPlayIndex-1;
						if (currentPlayIndex==-1){
							currentPlayIndex=tableSong.getItems().size()-1;
						}
						this.mediaAutoPlay(tableSong.getItems().get(currentPlayIndex));
					}
				}
			}
		});

		// 开始播放label
		playView = new ImageView("image/NeteasePause.png");
		playView.setFitHeight(32);
		playView.setFitWidth(32);
		Label labPlay = new Label("", playView);
		labPlay.setPrefWidth(32);
		labPlay.setPrefHeight(32);
		labPlay.getStylesheets().add("css/LabelScaleStyle.css");

		// 为开始播放label添加鼠标事件

		labPlay.setOnMouseClicked(e->{
			if(mediaPlayer!=null) {
				if(mediaPlayer.getStatus()==MediaPlayer.Status.PLAYING) {
					mediaPlayer.pause();
					playView.setImage(new Image("image/NeteasePause.png"));
				}
				else if(mediaPlayer.getStatus()==MediaPlayer.Status.PAUSED||mediaPlayer.getStatus()==MediaPlayer.Status.READY) {
					mediaPlayer.play();
					playView.setImage(new Image("image/NeteasePlay.png"));
				}
			}
			else{
				media = new Media(new File("/media/localdisk/Music/邓紫棋 - 喜欢你.wav").toURI().toString());
				mediaPlayer = new MediaPlayer(media);
				mediaPlayer.setOnReady(()->{
					mediaPlayer.play();
				});
			}
		});

		// 下一首label
		ImageView nextView = new ImageView("image/NeteaseNext.png");
		nextView.setFitHeight(30);
		nextView.setFitWidth(30);
		Label labPlayNext = new Label("", nextView);
		labPlayNext.setPrefWidth(30);
		labPlayNext.setPrefHeight(30);
		labPlayNext.getStylesheets().add("css/LabelScaleStyle.css");
		// 为下一首label添加鼠标事件
		labPlayNext.setOnMouseClicked(e->{
			if(mediaPlayer!=null){
				//表格的歌曲只有一首歌时执行的处理
				if (songsInfo.size()==1){
					//重新播放第一首歌，即原来的这首歌
					currentPlayIndex=0;
					this.mediaAutoPlay(tableSong.getItems().get(currentPlayIndex));
				}
				//否则表格的歌曲大于1，播放下一首歌曲
				else {
					//如果当前播放模式是“随机播放”，再次判断是否有下一首需要播放的歌曲
					if(this.currentPlayMode.equals("随机播放")){
						lastPlayIndexList.add(currentPlayIndex);
						//nextPlayIndexList的大小等0，证明当前没有需要播放下一首歌曲的索引，直接生成随机数播放
						if(nextPlayIndexList.size()==0){
							//先记录当前的索引是上一首需要的索引

							//然后生成一个随机数不是当前播放的索引值，执行播放
							while (true){
								int randomIndex=new Random().nextInt(tableSong.getItems().size());
								if (randomIndex!=currentPlayIndex){
									currentPlayIndex=randomIndex;
									break;
								}
							}
							this.mediaAutoPlay(tableSong.getItems().get(currentPlayIndex));
						}
						else{
							this.mediaAutoPlay(tableSong.getItems().get(nextPlayIndexList.get(nextPlayIndexList.size()-1)));
							currentPlayIndex = nextPlayIndexList.get(nextPlayIndexList.size()-1);
							nextPlayIndexList.remove(nextPlayIndexList.size()-1);
						}
					}
					//否则其它的模式都是当前的索引值+1执行播放
					else {
						currentPlayIndex=currentPlayIndex+1;
						if(currentPlayIndex>tableSong.getItems().size()-1){
							currentPlayIndex=0;
						}
						this.mediaAutoPlay(tableSong.getItems().get(currentPlayIndex));
					}
				}
			}
		});

		HBox leftHBox = new HBox(20);
		leftHBox.setAlignment(Pos.CENTER);
		leftHBox.setPadding(new Insets(0, 29, 0, 0));
//		leftHBox.setBorder(new Border(new BorderStroke(Color.WHITE, BorderStrokeStyle.SOLID, null, new BorderWidths(1))));
		leftHBox.getChildren().addAll(labAlbum,labPlayLast, labPlay, labPlayNext);



		// 播放时间label
		labPlayedTime = new Label("00:00");
//		labPlayedTime.setPrefHeight(40);
		labPlayedTime.setFont(new Font("宋体",9));
		labPlayedTime.setTextFill(Color.rgb(102, 102, 102));
		// 播放滑动条
		songSlider = new Slider();
		songSlider.getStylesheets().add("css/SliderAndProgressBar.css");



		// 播放进度条
		songProgressBar = new ProgressBar();
		songProgressBar.setProgress(0);
		songProgressBar.getStylesheets().add("css/SliderAndProgressBar.css");
		// StackPane容器包裹播放滑动条和播放进度条
		StackPane stackPane = new StackPane();
		stackPane.getChildren().addAll(songProgressBar, songSlider);
//		stackPane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(1))));
		stackPane.setPadding(new Insets(0,0,11,0));
		//为播放活动条设置监听事件

		songSlider.setOnMouseReleased(e->{
			if(mediaPlayer!=null) {
				if(songSlider.isFocused()){
					double playTimeValue = songSlider.getValue();
					mediaPlayer.seek(new Duration(1000*playTimeValue));
				}

			}
		});
		//sliderSong的值改变添加监听器，设置progressBarSong进度条的值
		songSlider.valueProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				if(mediaPlayer!=null) {
					if(songSlider.isPressed()&&!songSlider.isValueChanging()) {
						mediaPlayer.seek(new Duration(1000*songSlider.getValue()));
					}
					Date date = new Date((int)newValue.doubleValue()*1000); //乘以一千变成秒数
					labPlayedTime.setText(new SimpleDateFormat("mm:ss").format(date));
					songProgressBar.setProgress(newValue.doubleValue()/mediaPlayer.getTotalDuration().toSeconds());
				}
			}

		});
		//播放时间和总时间分隔号
		Label lab1 = new Label("/");
		lab1.setFont(new Font("宋体",10));
		lab1.setTextFill(Color.rgb(151, 151, 151));
		// 歌曲总时间label
		labTotalTime = new Label("00:00");
		labTotalTime.setTextFill(Color.rgb(151, 151, 151));
		labTotalTime.setFont(new Font("宋体",9));
//		labTotalTime.setPrefHeight(40);
//		labTotalTime.setPadding(new Insets(0,10,0,2));
		BorderPane centerBorderPane = new BorderPane();
		songSlider.prefWidthProperty().bind(centerBorderPane.widthProperty());
		songProgressBar.prefWidthProperty().bind(centerBorderPane.widthProperty());

		labMusicName = new Label("未知");
		labMusicName.setTextFill(Color.rgb(102, 102, 102));
		labMusicName.setMaxWidth(150);
		Label lab2 = new Label("-");
		lab2.setTextFill(Color.rgb(151, 151, 151));
		labSinger = new Label("未知");
		labSinger.setTextFill(Color.rgb(151, 151, 151));
		labSinger.setMaxWidth(150);

		HBox topRightHBox = new HBox(3);
		topRightHBox.setPadding(new Insets(10,0,0,0));
		topRightHBox.setAlignment(Pos.CENTER_RIGHT);
//		topHBox.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(1))));
		topRightHBox.getChildren().addAll(labPlayedTime,lab1,labTotalTime);
		HBox topLeftHBox = new HBox(3);
		topLeftHBox.setPadding(new Insets(10,0,0,0));
		topLeftHBox.getChildren().addAll(labMusicName,lab2,labSinger);
		BorderPane topBorderPane = new BorderPane();
		topBorderPane.setLeft(topLeftHBox);
		topBorderPane.setRight(topRightHBox);

		centerBorderPane.setTop(topBorderPane);
		centerBorderPane.setCenter(stackPane);

		// 音量图标
		ImageView soundView = new ImageView("image/NeteaseVolumeIcon.png");
		soundView.setFitWidth(19);
		soundView.setFitHeight(19);
		Label labSoundIcon = new Label("", soundView);
		labSoundIcon.setPrefHeight(19);
		labSoundIcon.setPadding(new Insets(0, 0, 0, 10));
		labSoundIcon.setOnMouseClicked(e->{
			if(mediaPlayer!=null) {
				if(mediaPlayer.isMute()) {
					mediaPlayer.setMute(false);
					volumeSlider.setValue(mediaPlayer.getVolume());
					soundView.setImage(new Image("image/NeteaseVolumeIcon.png"));
				}
				else {
					volumeSlider.setValue(0);
					mediaPlayer.setMute(true);
					soundView.setImage(new Image("image/NeteaseVolumeMuteIcon.png"));
				}
			}
		});

		// 音量滚动条
		volumeSlider = new Slider();
		volumeSlider.setMax(1);
		volumeSlider.setValue(0.05);
//		volumeSlider.setMajorTickUnit(0.01);// 每前进一格，增加多少的值
		volumeSlider.setPrefWidth(100);
		volumeSlider.getStylesheets().add("css/SliderAndProgressBar.css");

		// 音量进度条
		volumeProgressBar = new ProgressBar();
		volumeProgressBar.setProgress(0.05);
		volumeProgressBar.prefWidthProperty().bind(volumeSlider.prefWidthProperty());
		volumeProgressBar.getStylesheets().add("css/SliderAndProgressBar.css");

		StackPane volumeStackPane = new StackPane();
		volumeStackPane.getChildren().addAll(volumeProgressBar, volumeSlider);

		//设置音量滚动条的监听事件，使进度条始终跟随滚动条更新
		volumeSlider.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				volumeProgressBar.setProgress(newValue.doubleValue());
			}
		});
		//为音量滑动条设置鼠标拖拽、按下监听事件
		volumeSlider.setOnMouseDragged(e->{
			if(mediaPlayer!=null){
				if(volumeSlider.getValue()==0.0) {
					soundView.setImage(new Image("image/NeteaseVolumeMuteIcon.png"));
					labSoundIcon.setMouseTransparent(true);
				}
				else {
					if(mediaPlayer.isMute()){
						mediaPlayer.setMute(false);
					}
					soundView.setImage(new Image("image/NeteaseVolumeIcon.png"));
					labSoundIcon.setMouseTransparent(false);
				}
				mediaPlayer.setVolume(volumeSlider.getValue());
			}
		});
		volumeSlider.setOnMousePressed(e->{
			if(mediaPlayer!=null) {
				double volumeValue = volumeSlider.getValue();
				if(volumeValue==0.0) {
					soundView.setImage(new Image("image/NeteaseVolumeMuteIcon.png"));
				}
				else {
					soundView.setImage(new Image("image/NeteaseVolumeIcon.png"));
				}
				mediaPlayer.setVolume(volumeValue);
			}
		});

		// 播放模式图片
		ImageView playPatternView = new ImageView("image/NeteaseRandomPlayMode.png");
		playPatternView.setFitWidth(24);
		playPatternView.setFitHeight(24);
		Label labPlayPattern = new Label("", playPatternView);
		labPlayPattern.setPrefHeight(20);
		labPlayPattern.setOnMouseClicked(e->{
			switch (currentPlayMode){
				case "随机播放": {
					//显示播放模式动画的Label
					Label fadingShowPlayMode = new Label("顺序播放");
					//传入label，完成播放模式切换的动画提示
					this.ToastPlayModeInfo(fadingShowPlayMode);
					currentPlayMode="顺序播放";
					playPatternView.setImage(new Image("image/NeteaseSequencePlayMode.png"));
					break;
				}
				case "顺序播放":{
					//显示播放模式动画的Label
					Label fadingShowPlayMode = new Label("列表循环");
					//传入label，完成播放模式切换的动画提示
					this.ToastPlayModeInfo(fadingShowPlayMode);
					currentPlayMode="列表循环";
					playPatternView.setImage(new Image("image/NeteaseSequenceRoopMode.png"));
					break;
				}
				case "列表循环":{
					//显示播放模式动画的Label
					Label fadingShowPlayMode = new Label("单曲循环");
					//传入label，完成播放模式切换的动画提示
					this.ToastPlayModeInfo(fadingShowPlayMode);
					currentPlayMode="单曲循环";
					playPatternView.setImage(new Image("image/NeteaseSingleRoopIcon.png"));
					break;
				}
				case "单曲循环":{
					//显示播放模式动画的Label
					Label fadingShowPlayMode = new Label("随机播放");
					//传入label，完成播放模式切换的动画提示
					this.ToastPlayModeInfo(fadingShowPlayMode);
					currentPlayMode="随机播放";
					playPatternView.setImage(new Image("image/NeteaseRandomPlayMode.png"));
					break;
				}
				default:
			}
		});

		// 歌词按钮图片
		ImageView lyricView = new ImageView("image/ciDark.png");
		lyricView.setFitWidth(20);
		lyricView.setFitHeight(20);
		Label labLyricView = new Label("", lyricView);
		labLyricView.setPrefWidth(20);
		labLyricView.setPrefHeight(20);
		labLyricView.setPadding(new Insets(0, 16, 0, 0));

		HBox rightHBox = new HBox(10);
		rightHBox.setAlignment(Pos.CENTER);
//		rightHBox.setPadding(new Insets(0,0,0,10));
//		rightHBox.setBorder(new Border(null,new BorderStroke(Color.rgb(112,112,112) , null, null, null,null, BorderStrokeStyle.SOLID,null, null, null, new BorderWidths(1), null)));

		rightHBox.getChildren().addAll(labSoundIcon, volumeStackPane, labPlayPattern, labLyricView);

		BorderPane borderPane = new BorderPane();
		borderPane.setPrefHeight(60);
		borderPane.setLeft(leftHBox);
		borderPane.setCenter(centerBorderPane);
		borderPane.setRight(rightHBox);
		borderPane.setBackground(new Background(new BackgroundFill(Color.rgb(255, 255, 255), null, null)));
		borderPane.setBorder(new Border(new BorderStroke(Color.rgb(228, 228, 231), null, null, null,
				BorderStrokeStyle.SOLID, null, null, null, null, new BorderWidths(1,0,0,0), null)));
		return borderPane;
	}

	//传入Tag标签的label图标和显示的文字创建一个HBoxTag
	private HBox createLeftHBoxTag(Label labelIcon,String groupName){
		Label labelName = new Label(groupName);
		labelName.setFont(new Font("宋体", 13));
		HBox leftHBoxTag = new HBox(12, labelIcon, labelName);
		leftHBoxTag.setPrefHeight(40);
		leftHBoxTag.setAlignment(Pos.CENTER_LEFT);
		leftHBoxTag.setPadding(new Insets(0, 0, 0, 6));

		leftHBoxTagList.add(leftHBoxTag);
		leftHBoxTag.setOnMouseEntered(ee -> {
			if (!leftHBoxTag.isMouseTransparent())
				leftHBoxTag.setBackground(
						new Background(new BackgroundFill(Color.rgb(232, 232, 232), null, null)));
		});
		leftHBoxTag.setOnMouseExited(ee -> {
			leftHBoxTag.setBackground(new Background(new BackgroundFill(Color.rgb(243, 243, 245), null, null)));
			for (HBox h : leftHBoxTagList) {
				if (h.isMouseTransparent())
					h.setBackground(
							new Background(new BackgroundFill(Color.rgb(221, 221, 225), null, null)));
			}
		});
		leftHBoxTag.setOnMouseClicked(ee -> {
			for (HBox h : leftHBoxTagList) {
				h.setMouseTransparent(false);
				h.setBackground(new Background(new BackgroundFill(Color.rgb(243, 243, 245), null, null)));
			}
			leftHBoxTag.setBackground(new Background(new BackgroundFill(Color.rgb(221, 221, 225), null, null)));
			leftHBoxTag.setMouseTransparent(true);
		});
		return leftHBoxTag;
	}
	//音乐播放的函数，并且在mediaPlayer播放结束后判断当前的播放模式，执行下一首的播放
	private void mediaAutoPlay(SongInfo song){
		//记录之前的mediaPlyer之前的音量和是否为静音状态的局部变量
		double volume=0;
		boolean isMute=false;
		if(mediaPlayer!=null){  //如果mediaPlayer存在对象实例，先释放资源
			isMute = mediaPlayer.isMute();     //记录mediaPlayer是否是静音状态
			volume = mediaPlayer.getVolume();  //记录播放上一首音乐的音量
			this.mediaDestroy();
		}
		this.playView.setImage(new Image("image/NeteasePlay.png"));
		this.labMusicName.setText(song.getMusicName());
		this.labSinger.setText(song.getSinger());
		this.songSlider.setMax(song.getTotalSeconds());
		this.labTotalTime.setText(song.getTotalTime());
		media = new Media(new File(song.getSrc()).toURI().toString());
		mediaPlayer = new MediaPlayer(media);
		if(volume!=0&&isMute){  //如果之前的mediaPlayer是静音状态，那么设置当前的mediaPlayer也是静音状态，音量为上一个mediaPlayer的音量
			mediaPlayer.setMute(true);
			mediaPlayer.setVolume(volume);
		}
		else{
			mediaPlayer.setVolume(volumeSlider.getValue());
		}
		//播放器准备就绪执行播放
		mediaPlayer.setOnReady(()->{
			mediaPlayer.play();
		});
		//给播放器添加当前播放时间的监听器，更新当前播放进度条的信息
		mediaPlayer.currentTimeProperty().addListener(new ChangeListener<Duration>() {
			@Override
			public void changed(ObservableValue<? extends Duration> observable, Duration oldValue,
								Duration newValue) {
				if(!songSlider.isPressed()) {
					songSlider.setValue(newValue.toSeconds());
				}
			}
		});
		//播放器到结束时执行的操作
		mediaPlayer.setOnEndOfMedia(()->{
			switch (currentPlayMode){
				case "单曲循环":{
					mediaPlayer.seek(new Duration(0));  //定位到0毫秒(0秒)的时间，重新开始播放
					mediaPlayer.play();
					break;
				}
				case "顺序播放":{
					//顺序播放模式下，如果歌曲表格只有一首歌，那就定位到0毫秒(0秒)的时间，等待下一次播放
					if(tableSong.getItems().size()==1){
						mediaPlayer.seek(new Duration(0));
					}
					//否则，歌曲表格大于1，顺序播放，直到最后一首歌播放结束后，释放media资源
					else{
						//如果下一首的索引在表格中，播放下一首
						currentPlayIndex=currentPlayIndex+1;
						if(currentPlayIndex<=tableSong.getItems().size()-1){
							this.mediaAutoPlay(tableSong.getItems().get(currentPlayIndex));
						}
						//否则，定位到歌曲表格第一首歌，等待下一次播放
						else{
							this.mediaDestroy();
							this.playView.setImage(new Image("image/NeteasePause.png"));  //设置暂停状态的图标
							this.labMusicName.setText(tableSong.getItems().get(0).getMusicName());
							this.labSinger.setText(tableSong.getItems().get(0).getSinger());
							this.songSlider.setMax(tableSong.getItems().get(0).getTotalSeconds());
							this.labTotalTime.setText(tableSong.getItems().get(0).getTotalTime());
							media = new Media(new File(tableSong.getItems().get(0).getSrc()).toURI().toString());
							mediaPlayer = new MediaPlayer(media);
							mediaPlayer.setVolume(volumeSlider.getValue());
						}
					}
					break;
				}
				case "列表循环":{
					//列表循环模式下，如果歌曲表格只有一首歌，只要把mediaPlayer的当前播放时间重新设置为0秒就可以了
					if(tableSong.getItems().size()==1){
						mediaPlayer.seek(new Duration(0));  //定位到0毫秒(0秒)的时间，重新开始播放
						mediaPlayer.play();
					}
					//否则，执行下一首歌曲播放，形成循环列表
					else{
						currentPlayIndex=currentPlayIndex+1;
						if(currentPlayIndex>tableSong.getItems().size()-1){  //如果当前索引越界，值为0，形成一个循环
							currentPlayIndex=0;
						}
						this.mediaAutoPlay(tableSong.getItems().get(currentPlayIndex));
					}
					break;
				}
				case "随机播放":{
					//随机播放模式下，如果歌曲表格只有一首歌，只要把mediaPlayer的当前播放时间重新设置为0秒就可以了
					if(tableSong.getItems().size()==1){
						mediaPlayer.seek(new Duration(0));  //定位到0毫秒(0秒)的时间，重新开始播放
						mediaPlayer.play();
					}
					//否则，歌曲表格大于1，生成一个非当前播放的索引值来播放
					else{
						lastPlayIndexList.add(currentPlayIndex);
						//nextPlayIndexList的大小等0，证明当前没有需要播放下一首歌曲的索引，直接生成随机数播放
						if(nextPlayIndexList.size()==0){
							//先记录当前的索引是上一首需要的索引

							//然后生成一个随机数不是当前播放的索引值，执行播放
							while (true){
								int randomIndex=new Random().nextInt(tableSong.getItems().size());
								if (randomIndex!=currentPlayIndex){
									currentPlayIndex=randomIndex;
									break;
								}
							}
							this.mediaAutoPlay(tableSong.getItems().get(currentPlayIndex));
						}
						else{
							int index = nextPlayIndexList.size()-1;
							this.mediaAutoPlay(tableSong.getItems().get(nextPlayIndexList.get(index)));
							currentPlayIndex = nextPlayIndexList.get(nextPlayIndexList.size()-1);
							nextPlayIndexList.remove(nextPlayIndexList.size()-1);
						}
					}
					break;
				}
				default:
			}
		});
	}
	//释放播放器资源的函数
	private void mediaDestroy(){
		if(mediaPlayer.getStatus()!= MediaPlayer.Status.PLAYING){
			this.playView.setImage(new Image("image/NeteasePause.png"));
		}
		labPlayedTime.setText("00:00");
		songSlider.setValue(0);
		mediaPlayer.stop();
		mediaPlayer.dispose();
		media=null;
		mediaPlayer=null;
		System.gc();
	}
	//弹出模式切换提示动画的函数
	private void ToastPlayModeInfo(Label fadingShowPlayMode){
		fadingShowPlayMode.getStylesheets().add("css/fadingLabelStyle.css");
		FadeTransition fadeTransition = new FadeTransition(Duration.seconds(2.5),fadingShowPlayMode);
		fadeTransition.setFromValue(1);
		fadeTransition.setToValue(0);
		stageStackPane.getChildren().add(fadingShowPlayMode);
		//动画完成后移除label组件
		fadeTransition.setOnFinished(fade->{
			stageStackPane.getChildren().remove(1);
		});
		//开始播放渐变动画提示
		fadeTransition.play();
	}
	//根据存储的文件读取所有记录的歌曲的信息
	private ObservableList<SongInfo> getSongsInfo(File storageFile) {
		//读取所有保存的“添加的目录”文件夹
		List<String> folderList = XMLUtils.getAllRecord(storageFile, "path");
		//记录表格的所有歌曲信息的Items，它是ObservableList类型，table调用方法setItems()就可以了
		ObservableList<SongInfo> songsInfo = FXCollections.observableArrayList();
		//筛选出每个folder里面的mp3歌曲文件
		for (String folder : folderList) {
			// 扫描folderList存储的磁盘路径下面的歌曲
			File folderFile = new File(folder);
			String[] files = folderFile.list(new FilenameFilter() {
				@Override
				public boolean accept(File file, String fileName) {
					if (fileName.endsWith(".mp3")) {
						return true;
					} else {
						return false;
					}
				}
			});
			//移除此folder（目录文件）下的所有的歌曲记录
			XMLUtils.removeChoseFolderSubElements(storageFile, folder);
			//把读取出来的所有MP3文件添加回此folder，作为此folder的子元素
			for(String fileName:files) {
				XMLUtils.addOneRecordToChoseFolder(storageFile, folder, "song", fileName);
			}
			// 读取ChoseFolder的元素（文件夹）下的所有歌曲记录
			List<String> songList = XMLUtils.getAllSong(storageFile, folder);
			//开始每个mp3文件解析
			for(String song:songList) {
				try {
					String songName = null;//歌名
					String singer = null;//歌手
					String album = null;//专辑名称
					String totalTime; //时长
					String size;   //文件大小
					int totalSeconds;   //总的秒数
					//读取文件大小
					File songFile = new File(folder,song);
					size=String.valueOf(songFile.length()/1024.0/1024.0);
					size=size.substring(0, size.indexOf(".")+3)+"MB";
					//设置日志的输出级别，音乐文件解析时有某些音乐文件会输出警告提示在控制台，关闭它方便调试
					Logger.getLogger("org.jaudiotagger").setLevel(Level.SEVERE);
					Logger.getLogger("org.jaudiotagger.tag").setLevel(Level.OFF);
					Logger.getLogger("org.jaudiotagger.audio.mp3.MP3File").setLevel(Level.OFF);
					Logger.getLogger("org.jaudiotagger.tag.id3.ID3v23Tag").setLevel(Level.OFF);

					MP3File mp3File=(MP3File)AudioFileIO.read(songFile);
					MP3AudioHeader mp3AudioHeader = (MP3AudioHeader)mp3File.getAudioHeader();
					totalTime=mp3AudioHeader.getTrackLengthAsString();  //读取总时长，返回字符串类型，如“04：30”
					totalSeconds=mp3AudioHeader.getTrackLength();       //读取总时长，返回int类型的秒数，如270
					if(mp3File.hasID3v2Tag()) {
						Set<String> keySet = mp3File.getID3v2Tag().frameMap.keySet();
						if(keySet.contains("TIT2")){ //读取歌名
							songName = mp3File.getID3v2Tag().frameMap.get("TIT2").toString();
							if(songName!=null&&!songName.equals("null")) {
								songName=songName.substring(songName.indexOf("\"")+1, songName.lastIndexOf("\""));
							}
						}
						if(keySet.contains("TPE1")){  //读取歌手
							singer = mp3File.getID3v2Tag().frameMap.get("TPE1").toString();
							if(singer!=null&&!singer.equals("null")) {
								singer=singer.substring(singer.indexOf("\"")+1, singer.lastIndexOf("\""));
							}
						}
						if(keySet.contains("TALB")){  //读取专辑名
							album = mp3File.getID3v2Tag().frameMap.get("TALB").toString();
							if(album!=null&&!album.equals("null")) {
								album=album.substring(album.indexOf("\"")+1, album.lastIndexOf("\""));
							}
						}
					}
					else if(mp3File.hasID3v1Tag()) {
						ID3v1Tag id3v1Tag = mp3File.getID3v1Tag();
						songName = id3v1Tag.getFirst(FieldKey.TITLE);
						singer = id3v1Tag.getFirst(FieldKey.ARTIST);
						album = id3v1Tag.getFirst(FieldKey.ALBUM);
					}
					if(songName==null) {
						songName="";
					}
					if(singer==null) {
						singer="";
					}
					if(album==null) {
						album="";
					}
					if(totalTime==null) {
						totalTime="";
					}
					//创建MP3文件对象，设置歌名，歌手，专辑等信息
					SongInfo songObject=new SongInfo(songName, singer, album, totalTime, size);
					songObject.setSrc(folder+File.separator+song);     //设置歌曲路径
					songObject.setTotalSeconds(totalSeconds);	//设置歌曲总的秒数
					//把歌曲对象添加到集合
					songsInfo.add(songObject);
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (TagException e1) {
					e1.printStackTrace();
				} catch (ReadOnlyFileException e1) {
					e1.printStackTrace();
				} catch (CannotReadException e1) {
					e1.printStackTrace();
				} catch (InvalidAudioFrameException e1) {
					e1.printStackTrace();
				}
			}
		}
		return songsInfo;
	}
	//阻止主舞台borderPane响应鼠标事件和改变不透明度的函数
	private void blockBorderPane(){
		//设置主舞台界面borderPane除了顶部的titleBar部分外，其它的部分都不响应鼠标事件
		this.borderPane.getLeft().setMouseTransparent(true);
		this.borderPane.getCenter().setMouseTransparent(true);
		this.borderPane.getBottom().setMouseTransparent(true);
		//顺便设置不透明色，方便提示
		this.borderPane.getLeft().setOpacity(0.4);
		this.borderPane.getCenter().setOpacity(0.4);
		this.borderPane.getBottom().setOpacity(0.4);
	}
	//释放主舞台borderPane响应鼠标事件和不透明度变为默认值的函数
	private void releaseBorderPane(){
		this.borderPane.getLeft().setMouseTransparent(false);
		this.borderPane.getCenter().setMouseTransparent(false);
		this.borderPane.getBottom().setMouseTransparent(false);

		this.borderPane.getLeft().setOpacity(1);
		this.borderPane.getCenter().setOpacity(1);
		this.borderPane.getBottom().setOpacity(1);
	}
}


