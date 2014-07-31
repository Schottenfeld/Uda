package UDAServer;
import java.io.Serializable;

// Object representing a subject
public class UDASubject implements Serializable
{
    public int ID;
    public String Name;
    public String Notes;
    public String Timezone;

    public UDASubject(int ID, String Name, String Notes, String Timezone)
    {
      this.ID = ID;
      this.Name = Name;
      this.Notes = Notes;
      this.Timezone = Timezone;
    }
}
