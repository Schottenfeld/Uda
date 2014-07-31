package UDAServer;
import java.io.Serializable;

// Object representing a set of chosen modules and subjects of a student.
public class UDAChosenSM implements Serializable
{
  public int[] SubjectID = new int[4];
  public String[] SubjectType = new String[4];
  public String[] SubjectName = new String[4];

  public String[] ModuleID = new String[10];
  public String[] ModuleName = new String[10];
  public int[] ModuleSubject = new int[10];

  public String Student;
  public int Stage;
  public int SubjectCount;
  public int ModuleCount;

  public void UDAChosenSM()
  {
    // Set the members to 0 or "".
    for (int i=0; i<=4; i++)
    {
      SubjectID[i] = 0;
      SubjectType[i] = "";
      SubjectName[i] = "";
    }
    for (int i=0; i<=10; i++)
    {
      ModuleID[i] = "";
      ModuleName[i] = "";
      ModuleSubject[i] = 0;
    }
    Student="";
    Stage=0;
    SubjectCount=0;
    ModuleCount=0;
  }
}
