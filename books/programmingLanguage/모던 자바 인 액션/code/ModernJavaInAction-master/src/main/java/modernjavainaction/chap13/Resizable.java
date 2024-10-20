package modernjavainaction.chap13;

public interface Resizable extends Drawable {

  int getWidth();
  int getHeight();
  void setWidth(int width);
  void setHeight(int height);
  void setAbsoluteSize(int width, int height);
  //TODO: 주석 해제, 자세한 사항은 README 참고
  //void setRelativeSize(int widthFactor, int heightFactor);

}
