package UDAServer;
import java.io.Serializable;

// Object representing a module
public class UDAModule implements Serializable
{
    public String Name="";
    public String Size="";
    public String Code="";
    public String Timeslot="";
    public String Essential="";

    public UDAModule(String Name, String Size, String Code, String Timeslot, String Essential)
    {
      this.Name = Name;
      this.Size = Size;
      this.Code = Code;
      this.Timeslot = Timeslot;
      this.Essential = Essential;
    }
}
