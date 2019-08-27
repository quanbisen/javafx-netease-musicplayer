package application;

public class SongInfo {

	private String musicName;  //歌名
	private String singer;     //歌手
	private String album;		//专辑
	private String totalTime;	//时长
	private String size;		//大小
	private String src;  //路径
	private String lyricsSrc;  //歌词路径
	private int totalSeconds;	//总秒数
	
	public SongInfo(String musicName,String singer,String album,String totalTime,String size) {
		this.musicName=musicName;
		this.singer=singer;
		this.album=album;
		this.totalTime=totalTime;
		this.size=size;
	}
	public String getMusicName() {
		return musicName;
	}
	public void setMusicName(String musicName) {
		this.musicName = musicName;
	}
	public String getSinger() {
		return singer;
	}
	public void setSinger(String singer) {
		this.singer = singer;
	}
	public String getAlbum() {
		return album;
	}
	public void setAlbum(String album) {
		this.album = album;
	}
	public String getSize() {
		return size;
	}
	public void setSize(String size) {
		this.size = size;
	}
	public String getTotalTime() {
		return totalTime;
	}
	public void setTotalTime(String totalTime) {
		this.totalTime = totalTime;
	}
	public String getSrc() {
		return src;
	}
	public void setSrc(String src) {
		this.src = src;
	}
	public String getLyricsSrc() {
		return lyricsSrc;
	}
	public void setLyricsSrc(String lyricsSrc) {
		this.lyricsSrc = lyricsSrc;
	}
	public int getTotalSeconds() {
		return totalSeconds;
	}
	public void setTotalSeconds(int totalSeconds) {
		this.totalSeconds = totalSeconds;
	}
}
