package com.c2t2s.hb;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*; //TODO: Remove the *
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.Random;

public class Wagers {
	
	public static class Wager {
		private int id;
		private long uid;
		private boolean closed;
		private String title;
		private List<String> options;
		
		public Wager(int id, long uid, boolean closed, String title, List<String> options) {
			this.id = id;
			this.uid = uid;
			this.closed = closed;
			this.title = title;
			this.options = options;
		}
		
		public int getId() {
			return id;
		}
		
		public long getUid() { 
			return uid;
		}
		
		public List<String> getOptions() {
			return options;
		}
		
		public int getOptionsCount() {
			return options.size();
		}
		
		public boolean isClosed() {
			return closed;
		}
		
		public String getTitle() {
			return title;
		}
	}
	
	public static String createWager(long uid, String title, List<String> options) {
		if (title == null || title.equals("")) {
			return "Unable to create new wager: Title is required";
		} else if (title.length() > 100) {
			return "Unable to create new wager: Titles can be no longer than 100 characters";
		}
		if (options.size() < 2) {
			return "Unable to create new wager: Wagers require between 2 and 10 options";
		} else if (options.size() > 10) {
			return "Unable to create new wager: Maximum number of options is 10";
		}
		for (int i = 0; i < options.size(); ++i) {
			if (options.get(i).length() > 100) {
				return "Unable to create new wager: Option " + i + " is longer than the maximum 100 characters";
			}
		}
		int optionsCount = options.size();
		for (int i = 0; i < (10 - optionsCount); ++i) {
			options.add("");
		}
		int wagerId = addWager(uid, title, options);
		if (wagerId < 0) {
			return "Unable to create new wager due to a database error";
		} else {
			String output = "Created new wager #" + wagerId + ": " + title;
			for (int i = 1; i <= optionsCount; ++i) {
				output += "\n\tOption " + i + ": " + options.get(i);
			}
			return output;
		}
	}
	
	public static String setClosed(long uid, int id, boolean isClosed) {
		Wager wager = getWager(id);
		if (wager == null || wager.getId() < 1) {
			return "Unable to find a wager with id " + id;
		}
		if (wager.getUid() != uid) {
			return "Unable to " + (isClosed ? "close" : "open") + " wager #"
		        + id + ": you are not the creator of that wager";
		}
		setWagerClosed(id, isClosed);
		return (isClosed ? "Closed" : "Opened") + " wager #" + id;
	}
	
	public static String payoutWager(long uid, int id, int correct) {
		Wager wager = getWager(id);
		if (wager == null || wager.getId() < 1) {
			return "Unable to find a wager with id " + id;
		}
		if (wager.getUid() != uid) {
			return "Unable to payout wager #" + id + ": You are not the creator of that wager";
		} else if (wager.getOptionsCount() < correct) {
			return "Unable to payout wager #" + id + ": There are not " + correct + " options";
		}
		String output = payoutWager(id, correct);
		if (!output.isEmpty()) {
			deleteWager(id);
		}
		return output;
	}
	
	public static String placeBet(long uid, int id, int option, long bet) {
		Wager wager = getWager(id);
		if (wager == null || wager.getId() < 1) {
			return "Unable to find a wager with id " + id;
		} else if (wager.isClosed()) {
			return "Unable to place a bet for wager #" + id + ": Wager is closed";
		} else if (wager.getOptionsCount() < option) {
			return "Unable to place bet for wager #" + id + ": There are not " + option + " options";
		}
		long balance = Casino.checkBalance(uid);
		if (balance < 0) {
			return "Unable to place bet: Balance check failed or was negative (" + balance +")";
		} else if (balance < bet) {
			return "Your current balance of " + balance + " is not enough to cover that";
		}
		long totalBet = getBet(uid, id, option);
		if (totalBet > 0) {
			Casino.takeMoneyDirect(uid, bet);
			totalBet = increaseBet(uid, id, option, bet);
			return "Increased bet on option " + option + " of wager #" + id + " to " + totalBet;
		} else {
			if (createBet(uid, id, option, bet) < 1) {
				return "Unable to place bet due to a database error";
			}
			Casino.takeMoneyDirect(uid, bet);
			return "Placed a bet of " + bet + " on \"" + wager.getOptions().get(option - 1) + "\"";
		}
	}
	
	public static String getWagerInfo(int id) {
		Wager wager = getWager(id);
		if (wager == null || wager.getId() < 1) {
			return "No wager found with id " + id;
		}
		String output = "Wager #" + id + ": " + wager.getTitle();
		for (int i = 1; i <= wager.getOptionsCount(); ++i) {
			output += "\n\t" + i + ": " + wager.getOptions().get(i - 1);
		}
		return output;
	}
	
	public static String openWagers() {
		String output = getOpenWagers();
		if (output.isEmpty()) {
			return "No open wagers found";
		}
		return "Open wagers:" + output;
	}
	
	//////////////////////////////////////////////////////////
	
    private static Connection getConnection() throws URISyntaxException, SQLException {
        return DriverManager.getConnection(System.getenv("JDBC_DATABASE_URL"));
    }
    
    //CREATE TABLE IF NOT EXISTS wager_user (
    //  uid bigint PRIMARY KEY,
    //  bets integer DEFAULT 0,
    //  correct integer DEFAULT 0,
    //  hosted integer DEFAULT 0,
    //  spent bigint DEFAULT 0,
    //  winnings bigint DEFAULT 0,
    //  CONSTRAINT wageruser_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    //);
	
	//CREATE TABLE IF NOT EXISTS wagers (
    //  id SERIAL PRIMARY KEY,
	//	uid bigint,
    //  closed boolean DEFAULT false,
    //  title varchar(100),
    //  option1 varchar(100),
    //  option2 varchar(100),
    //  option3 varchar(100) DEFAULT '',
    //  option4 varchar(100) DEFAULT '',
    //  option5 varchar(100) DEFAULT '',
    //  option6 varchar(100) DEFAULT '',
    //  option7 varchar(100) DEFAULT '',
    //  option8 varchar(100) DEFAULT '',
    //  option9 varchar(100) DEFAULT '',
    //  option10 varchar(100) DEFAULT '',
    //  CONSTRAINT wagers_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
	//);
    
    //CREATE TABLE IF NOT EXISTS bets (
    //  uid bigint,
    //  id integer,
    //  option smallint,
    //  bet integer,
    //  PRIMARY KEY(uid, id, option),
    //  CONSTRAINT bets_uid FOREIGN KEY(uid) REFERENCES money_user(uid),
    //  CONSTRAINT bets_wager_id FOREIGN KEY(id) REFERENCES wagers(id) ON DELETE CASCADE
    //);
    
    private static int addWager(long uid, String title, List<String> options) {
    	if (options.size() < 10) {
    		return -1;
    	}
		String query = "INSERT INTO wagers (uid, title, option1, option2, option3, option4, option5, option6, option7, option8, option9, option10) VALUES ("
    	    + uid + ", '?' , '?', '?', '?', '?', '?', '?', '?', '?', '?', '?') RETURNING id;";
		String updateQuery = "UPDATE wager_user SET hosted = hosted + 1 WHERE uid = " + uid;
        Connection connection = null;
        PreparedStatement statement = null;
        int id = -1;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, title);
            for (int i = 2; i <= 11; i++) {
            	statement.setString(i, options.get(i - 2));
            }
            ResultSet results = statement.executeQuery();
            if (results.next()) {
            	id = results.getInt(1);
            }
            statement.executeUpdate(updateQuery);
            statement.close();
            Statement updateStatement = connection.createStatement();
            updateStatement.executeUpdate(updateQuery);
            updateStatement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
        	e.printStackTrace();
        	System.out.println(options);
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return id;
    }
    
    private static void setWagerClosed(int id, boolean closed) {
        executeUpdate("UPDATE wagers SET closed = " + closed + " WHERE id = " + id + ";");
    }
    
    private static void deleteWager(int id) {
    	executeUpdate("DELETE FROM wagers WHERE id = " + id + ";");
    }
    
    private static String payoutWager(int id, int correct) {
    	String potQuery = "SELECT SUM(bet) FROM bets WHERE id = " + id + ";";
    	String winnersContributionQuery = "SELECT SUM(bet) FROM bets WHERE id = " + id + " AND option = " + correct + ";";
        String fetchWinners = "SELECT uid, bet, name FROM bets NATURAL JOIN money_user WHERE id = " + id + " AND option = " + correct + ";";
        Connection connection = null;
        Statement statement = null;
        long pot, winnerContribution, payout, totalPaid = 0, uid;
        String output = "";
        try {
            connection = getConnection();
            statement = connection.createStatement();
            pot = statement.executeQuery(potQuery).getLong(1);
            if (pot == 0) {
            	output = "No bets were placed.";
            } else {
                output += "Paying out pot of " + pot + " coins:";
                winnerContribution = statement.executeQuery(winnersContributionQuery).getLong(1);
                if (winnerContribution == 0) {
            	    output = "\nNo correct bets were placed, feeding the pot to the money machine";
            	    statement.executeUpdate("UPDATE money_user SET balance = balance + "
                		    + pot + " WHERE uid = " + Casino.MONEY_MACHINE_UID + ";");
                } else {
                	double payoutRatio = pot / winnerContribution;
                	int option;
                	ResultSet results = statement.executeQuery(fetchWinners);
                	while (results.next()) {
                		uid = results.getLong(1);
                		payout = (long)(results.getLong(2) * payoutRatio);
                		String name = results.getString(3);
                		statement.executeUpdate("UPDATE money_user SET balance = balance + "
                		    + payout + " WHERE uid = " + uid + ";");
                		statement.executeUpdate("UPDATE wager_user SET (correct, winnings) = (correct + 1, winnings + "
                		    + payout + ") WHERE uid = " + uid + ";");
                		totalPaid += payout;
                		output += "\n" + name + " won " + payout;
                	}
                	results.close();
                	if (totalPaid < pot) {
                		statement.executeUpdate("UPDATE money_user SET balance = balance + "
                    		    + (pot - totalPaid) + " WHERE uid = " + Casino.MONEY_MACHINE_UID + ";");
                	}
                }
            }
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return output;
    }
    
    private static int createBet(long uid, int wagerId, int option, long bet) {
    	String query = "INSERT INTO bets (uid, id, option, bet) VALUES (" + uid + ", "
            + ", " + wagerId + ", " + option + ", " + bet + ") ON CONFLICT (uid, id, option) DO NOTHING;";
        String trackingQuery = "UPDATE wager_user SET (bets, spent) = (bets + 1, spent + "
            + bet + ") WHERE uid = " + uid + ";";
        Connection connection = null;
        Statement statement = null;
        int count = -1;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            count = statement.executeUpdate(query);
            statement.executeUpdate(trackingQuery);
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return count;
    }
    
    private static long increaseBet(long uid, int id, int option, long bet) {
    	String query = "UPDATE bets SET bet = bet + " + bet + " WHERE (uid, id, option) = ("
            + uid + ", " + id + ", " + option + ") RETURNING bet;";
    	Connection connection = null;
        Statement statement = null;
        long total = -1;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            total = statement.executeUpdate(query);
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return total;
    }
    
    private static Wager getWager(int id) {
    	String query = "SELECT id, uid, closed, title, option1, option2, option3, option4, option5, option6, option7, option8, option9, option10 FROM wagers WHERE id = " + id + ";";
		Connection connection = null;
        Statement statement = null;
        Wager wager = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            if (results.next()) {
            	int rowId = results.getInt(1);
            	long uid = results.getLong(2);
            	boolean closed = results.getBoolean(3);
            	String title = results.getString(4);
            	List<String> options = new ArrayList();
            	String option;
            	boolean lastOption = false;
            	for (int i = 5; i < 15 && !lastOption; ++i) {
            		option = results.getString(i);
            		if (option.equals("")) {
            			lastOption = true;
            		} else {
            			options.add(option);
            		}
            	}
            	wager = new Wager(rowId, uid, closed, title, options);
            }
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return wager;
    }
    
    private static long getBet(long uid, int id, int option) {
    	String query = "SELECT bet FROM bets WHERE (uid, id, option) = (" + uid
    		+ ", " + id + ", " + option + ");";
        Connection connection = null;
        Statement statement = null;
        long wager = -1;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            if (results.next()) {
            	wager = results.getLong(1);
            }
            results.close();
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return wager;
    }
    
    private static String getOpenWagers() {
    	String query = "SELECT id, title FROM wagers WHERE closed = false;";
		Connection connection = null;
        Statement statement = null;
        String output = "";
        try {
            connection = getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            while (results.next()) {
            	int id = results.getInt(1);
            	String title = results.getString(2);
            	output += "\n\t" + id + ": " + title;
            	
            }
            results.close();
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return output;
    }

    private static boolean executeUpdate(String query) {
        boolean error = false;
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            statement.executeUpdate(query);
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return error;
    }

}
