package edu.application;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
 
/**
 * Created by wzj on 2017/8/22.
 */
public class GlobalMenu extends ContextMenu
{
    /**
     * 单例
     */
    private static GlobalMenu INSTANCE = null;
 
    /**
     * 私有构造函数
     */
    private GlobalMenu()
    {
        MenuItem settingMenuItem = new MenuItem("设置");
        MenuItem updateMenuItem = new MenuItem("检查更新");
        MenuItem feedbackMenuItem = new MenuItem("官方论坛");
        MenuItem aboutMenuItem = new MenuItem("问题与建议");
        MenuItem companyMenuItem = new MenuItem("关于");
        
 
        getItems().add(settingMenuItem);
        getItems().add(updateMenuItem);
        getItems().add(companyMenuItem);
        getItems().add(feedbackMenuItem);
        getItems().add(aboutMenuItem);
    }
 
    /**
     * 获取实例
     * @return GlobalMenu
     */
    public static GlobalMenu getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new GlobalMenu();
        }
 
        return INSTANCE;
    }
}
