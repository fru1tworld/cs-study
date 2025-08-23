package modernjavainaction.chap20;

public class Student {

  private String name;
  private int id;

  public static void main(String[] args) {
    Student s = new Student("Raoul", 1);
    System.out.println(s.name);
    s.id = 1337;
    System.out.println(s.id);
  }

  public Student() {}

  public Student(String name, int id) {
    this.name = name;
    this.id = id;
  }

  public Student(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

}
