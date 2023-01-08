package Users.Professors;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.SqlDateModel;
import DataBase.DBConnection;
import Exceptions.ExceptionFrame;
import MyLoader.RoomLoader;
import Notifications.AcceptRejectFrame;
import Notifications.ProfessorNotification;
import Notifications.ProfessorSwapDraft;
import Rooms.Rooms;
import Users.GeneralUser.Users;
import Users.GeneralUser.UsersGUI;

public class SendDraftFrame extends JFrame{
	private String[] startHours= {"From","9:00", "10:00", "11:00", "12:00", "14:00", "15:00", "16:00", "17:00"};
	private String selectedRoom;

	public SendDraftFrame(ProfessorNotification notification, Users user, UsersGUI frame) {
		JPanel secondPanel=new JPanel();
		JPanel mainPanel=new JPanel();
		mainPanel.setLayout(new BorderLayout());
		try {

			secondPanel.setLayout (new GridBagLayout());
			GridBagConstraints c=new GridBagConstraints();
			RoomLoader loadRooms=new RoomLoader();

			HashMap<String, Rooms> allRooms=loadRooms.getRooms();
			ArrayList<Rooms> roomsList=new ArrayList<Rooms>();
			for(HashMap.Entry<String, Rooms> entry : allRooms.entrySet()) {
				roomsList.add(entry.getValue());
			}
			Collections.sort(roomsList);
			JComboBox<String> roomsBox=new JComboBox(roomsList.toArray());

			JLabel label = new JLabel("Prepare your Draft Swap Proposal");
			mainPanel.add(label, BorderLayout.NORTH);

			SqlDateModel model= new SqlDateModel();
			Properties p = new Properties();
			p.put("text.today", "Today");
			p.put("text.month", "Month");
			p.put("text.year", "Year");
			JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
			c.gridx=0;
			c.gridy=0;
			secondPanel.add(datePanel, c);

			JComboBox<String> endTimeBox=new JComboBox<String>();
			endTimeBox.addItem("To");
			endTimeBox.setFocusable(false);
			c.gridx=2;
			c.gridy=0;
			secondPanel.add(endTimeBox, c);

			JComboBox<String> startTimeBox=new JComboBox<String>(startHours);
			ToActionListener sl=new ToActionListener(startTimeBox, endTimeBox);
			startTimeBox.addActionListener(sl);
			startTimeBox.setFocusable(false);
			c.gridx=1;
			c.gridy=0;
			secondPanel.add(startTimeBox, c);

			c.gridx=3;
			c.gridy=0;
			secondPanel.add(roomsBox, c);

			JButton send=new JButton("Send");
			send.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					selectedRoom=(String)roomsBox.getSelectedItem().toString();
					Date receiverDate=Date.valueOf(notification.getDate());
					Date draftDate=(Date)datePanel.getModel().getValue();

					try {

						if(receiverDate.compareTo(draftDate)>0) {
							throw new Exception();
						}
					}catch(Exception ex) {
						new ExceptionFrame("Not a valid Date!");
						return;
					}

					try {
						Connection conn=DBConnection.connect();
						LocalDate myDate = draftDate.toLocalDate();
						DayOfWeek dayOfWeek=myDate.getDayOfWeek();
						String query="select Schedule_ID from schedule where Start_Time=? and End_Time=? and Room=? and Day_Of_Week=?";
						PreparedStatement preparedStmt = conn.prepareStatement(query);
						preparedStmt.setString(1, startTimeBox.getSelectedItem().toString());
						preparedStmt.setString(2, endTimeBox.getSelectedItem().toString());
						preparedStmt.setString(3, selectedRoom);
						preparedStmt.setString(4, dayOfWeek.toString());
						ResultSet result=preparedStmt.executeQuery();
						result.next();
						String newSchedule=result.getString(1);

						query="insert into swap_notifications (Sender, Receiver, First_Date, New_Date, First_Schedule, New_Schedule, Accepted)"+"values(?, ?, ?, ?, ?, ?, ?)";
						preparedStmt = conn.prepareStatement(query);
						preparedStmt.setString(1, user.getID());
						preparedStmt.setString(2, notification.getSender());
						preparedStmt.setDate(3, receiverDate);
						preparedStmt.setDate(4, draftDate);
						preparedStmt.setString(5, notification.getScheduleID());
						preparedStmt.setString(6, newSchedule);
						preparedStmt.setString(7, "false");
						preparedStmt.execute();

						conn.close();

					} catch (Exception ea) {
						new ExceptionFrame("\u274C Error!");
						return;
					}
					notification.accept();
					new AcceptRejectFrame("Swap Proposal Sent!", user, frame);
					dispose();
				}
			});
			c.gridx=4;
			c.gridy=0;
			secondPanel.add(send, c);

		} catch (Exception e1) {
			e1.printStackTrace();
		}
		mainPanel.add(secondPanel, BorderLayout.CENTER);
		add(mainPanel);
		setSize(600,300);
		setTitle("Choose Room");

		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	public String getRoom() {
		return this.selectedRoom;
	}
}