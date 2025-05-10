import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.net.*;
import java.io.*;

public class Three_Mens_Morris extends JPanel implements MouseListener {
    // Game Constants
    private static final int BOARD_SIZE = 3;
    private static final int CELL_SIZE = 150;
    private static final int SPLASH_DURATION = 3000;
    
    private final int[][] Game_Board = new int[BOARD_SIZE][BOARD_SIZE];
    private int Current_Player = 1;
    private Point Selected_Piece = null;
    private boolean Is_Game_Over = false;
    private java.util.List<Point> Winning_Line = new ArrayList<>();
    private Set<Point> Player1_Moved_Pieces = new HashSet<>();
    private Set<Point> Player2_Moved_Pieces = new HashSet<>();
    private boolean Player1_All_Moved = false;
    private boolean Player2_All_Moved = false;
    
    private boolean Is_Online_Game = false;
    private boolean Is_Server = false;
    private Socket Game_Socket;
    private ObjectOutputStream Output_Stream;
    private ObjectInputStream Input_Stream;
    
    private final Rectangle Reset_Button = new Rectangle(300, BOARD_SIZE * CELL_SIZE + 20, 100, 30);
    private JMenuBar Game_Menu_Bar;
    private String Player1_Name = "Player 1";
    private String Player2_Name = "Player 2";
    private Color Player1_Color = Color.RED;
    private Color Player2_Color = Color.BLUE;
    private int Player1_Wins = 0;
    private int Player2_Wins = 0;
    private JLabel Score_Label;
    private ImageIcon Game_Icon;
    private JFrame Main_Frame;

    public Three_Mens_Morris() {
        Show_Splash_Screen();
    }

    private void Initialize_Game() {
        try {
            Game_Icon = new ImageIcon("p.png");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Icon file not found");
        }
        
        setPreferredSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE + 150));
        setBackground(new Color(210, 180, 140));
        addMouseListener(this);
        Initialize_Menu();
        Initialize_Board();
        Create_Main_Window();
    }

    private void Show_Splash_Screen() {
        JWindow splashWindow = new JWindow();
        splashWindow.setLayout(new BorderLayout());
        splashWindow.getContentPane().setBackground(Color.BLACK);

        ImageIcon splashIcon = new ImageIcon("p.png");
        JLabel iconLabel = new JLabel(splashIcon);

        JPanel splashPanel = new JPanel();
        splashPanel.setBackground(Color.BLACK);
        splashPanel.add(iconLabel);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(0, 255, 255));
        progressBar.setBackground(Color.DARK_GRAY);
        progressBar.setFont(new Font("Arial", Font.BOLD, 20));

        JLabel appNameLabel = new JLabel("Three Men's Morris", JLabel.CENTER);
        appNameLabel.setForeground(new Color(0, 255, 255));
        appNameLabel.setFont(new Font("Arial", Font.BOLD, 30));

        splashWindow.add(appNameLabel, BorderLayout.NORTH);
        splashWindow.add(splashPanel, BorderLayout.CENTER);
        splashWindow.add(progressBar, BorderLayout.SOUTH);
        splashWindow.setSize(500, 300);
        splashWindow.setLocationRelativeTo(null);
        splashWindow.setVisible(true);

        new Thread(() -> {
            Color[] colors = {
                new Color(0, 255, 255), 
                new Color(255, 0, 255), 
                new Color(255, 165, 0), 
                new Color(0, 255, 0)
            };
            int colorIndex = 0;
            for (int i = 0; i <= 100; i++) {
                try {
                    Thread.sleep(SPLASH_DURATION / 100);
                    progressBar.setValue(i);
                    progressBar.setForeground(colors[colorIndex]);
                    colorIndex = (colorIndex + 1) % colors.length;
                    iconLabel.setIcon(Rotate_Icon(splashIcon, i));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            splashWindow.dispose();
            Initialize_Game();
        }).start();
    }

    private ImageIcon Rotate_Icon(ImageIcon originalIcon, int degrees) {
        Image img = originalIcon.getImage();
        BufferedImage rotated = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        g2d.rotate(Math.toRadians(degrees), img.getWidth(null)/2, img.getHeight(null)/2);
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();
        return new ImageIcon(rotated);
    }

    private void Create_Main_Window() {
        Main_Frame = new JFrame("Three Men's Morris");
        if (Game_Icon != null) {
            Main_Frame.setIconImage(Game_Icon.getImage());
        }
        Main_Frame.setJMenuBar(Game_Menu_Bar);
        Main_Frame.add(Score_Label, BorderLayout.SOUTH);
        Main_Frame.add(this, BorderLayout.CENTER);
        Main_Frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Main_Frame.setResizable(false);
        Main_Frame.pack();
        Main_Frame.setLocationRelativeTo(null);
        Main_Frame.setVisible(true);
    }

    private void Initialize_Board() {
        for (int[] row : Game_Board) {
            Arrays.fill(row, 0);
        }

        // Initial piece placement
        Game_Board[0][0] = 1;
        Game_Board[0][1] = 1;
        Game_Board[0][2] = 1;
        Game_Board[2][0] = 2;
        Game_Board[2][1] = 2;
        Game_Board[2][2] = 2;

        Selected_Piece = null;
        Current_Player = 1;
        Is_Game_Over = false;
        Winning_Line = new ArrayList<>();
        Player1_Moved_Pieces.clear();
        Player2_Moved_Pieces.clear();
        Player1_All_Moved = false;
        Player2_All_Moved = false;
        
        repaint();
    }

    private void Initialize_Menu() {
        Game_Menu_Bar = new JMenuBar();
        
        JMenu gameMenu = new JMenu("Game");
        JMenuItem newGameItem = new JMenuItem("New Game");
        JMenuItem networkItem = new JMenuItem("Network Game");
        JMenuItem exitItem = new JMenuItem("Exit");
        
        newGameItem.addActionListener(e -> Initialize_Board());
        networkItem.addActionListener(e -> Setup_Network_Game());
        exitItem.addActionListener(e -> System.exit(0));
        
        gameMenu.add(newGameItem);
        gameMenu.add(networkItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);
        
        JMenu optionsMenu = new JMenu("Options");
        JMenuItem namesItem = new JMenuItem("Change Player Names");
        JMenuItem colorsItem = new JMenuItem("Change Colors");
        
        namesItem.addActionListener(e -> Change_Player_Names());
        colorsItem.addActionListener(e -> Change_Player_Colors());
        
        optionsMenu.add(namesItem);
        optionsMenu.add(colorsItem);
        
        JMenu helpMenu = new JMenu("Help");
        JMenuItem rulesItem = new JMenuItem("How to Play");
        JMenuItem aboutItem = new JMenuItem("About");
        
        rulesItem.addActionListener(e -> Show_Game_Rules());
        aboutItem.addActionListener(e -> Show_About_Dialog());
        helpMenu.add(rulesItem);
        helpMenu.add(aboutItem);
        
        Game_Menu_Bar.add(gameMenu);
        Game_Menu_Bar.add(optionsMenu);
        Game_Menu_Bar.add(helpMenu);
        
        Score_Label = new JLabel(Player1_Name + ": " + Player1_Wins + " | " + Player2_Name + ": " + Player2_Wins);
        Score_Label.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void Setup_Network_Game() {
        String[] options = {"Host Game", "Join Game"};
        int choice = JOptionPane.showOptionDialog(this, 
            "Start or join a network game", 
            "Network Game",
            JOptionPane.DEFAULT_OPTION, 
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);
        
        if (choice == 0) {
            Is_Server = true;
            new Thread(this::Start_Game_Server).start();
            JOptionPane.showMessageDialog(this, "Waiting for player to connect...");
        } else if (choice == 1) {
            String ip = JOptionPane.showInputDialog(this, "Enter server IP:");
            if (ip != null && !ip.isEmpty()) {
                new Thread(() -> Connect_To_Server(ip)).start();
            }
        }
    }

    private void Start_Game_Server() {
        try {
            ServerSocket serverSocket = new ServerSocket(1234);
            Game_Socket = serverSocket.accept();
            Output_Stream = new ObjectOutputStream(Game_Socket.getOutputStream());
            Input_Stream = new ObjectInputStream(Game_Socket.getInputStream());
            Is_Online_Game = true;
            Player1_Name = "Host";
            Player2_Name = "Client";
            Initialize_Board();
            JOptionPane.showMessageDialog(this, "Player connected!");
            Listen_For_Network_Moves();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Server error: " + e.getMessage());
        }
    }

    private void Connect_To_Server(String ip) {
        try {
            Game_Socket = new Socket(ip, 1234);
            Output_Stream = new ObjectOutputStream(Game_Socket.getOutputStream());
            Input_Stream = new ObjectInputStream(Game_Socket.getInputStream());
            Is_Online_Game = true;
            Player1_Name = "Client";
            Player2_Name = "Host";
            Initialize_Board();
            JOptionPane.showMessageDialog(this, "Connected to server!");
            Listen_For_Network_Moves();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Connection error: " + e.getMessage());
        }
    }

    private void Listen_For_Network_Moves() {
        new Thread(() -> {
            try {
                while (true) {
                    Game_Move move = (Game_Move) Input_Stream.readObject();
                    SwingUtilities.invokeLater(() -> {
                        Game_Board[move.To_Row][move.To_Col] = Current_Player;
                        Game_Board[move.From_Row][move.From_Col] = 0;
                        Current_Player = (Current_Player == 1) ? 2 : 1;
                        repaint();
                    });
                }
            } catch (Exception e) {
                System.out.println("Network error: " + e.getMessage());
            }
        }).start();
    }

    private void Send_Game_Move(int fromRow, int fromCol, int toRow, int toCol) {
        if (Is_Online_Game) {
            try {
                Output_Stream.writeObject(new Game_Move(fromRow, fromCol, toRow, toCol));
                Output_Stream.flush();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to send move");
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Draw_Game_Board(g);
        Draw_Game_Pieces(g);
        Draw_Player_Labels(g);
        Draw_Winning_Line(g);
        if (Is_Game_Over) Draw_Reset_Button(g);
    }

    private void Draw_Game_Board(Graphics g) {
        g.setColor(Color.BLACK);
        for (int i = 0; i < BOARD_SIZE; i++) {
            g.drawLine(CELL_SIZE / 2, i * CELL_SIZE + CELL_SIZE / 2,
                     BOARD_SIZE * CELL_SIZE - CELL_SIZE / 2, i * CELL_SIZE + CELL_SIZE / 2);
            g.drawLine(i * CELL_SIZE + CELL_SIZE / 2, CELL_SIZE / 2,
                     i * CELL_SIZE + CELL_SIZE / 2, BOARD_SIZE * CELL_SIZE - CELL_SIZE / 2);
        }
        g.drawLine(CELL_SIZE / 2, CELL_SIZE / 2,
                 BOARD_SIZE * CELL_SIZE - CELL_SIZE / 2, BOARD_SIZE * CELL_SIZE - CELL_SIZE / 2);
        g.drawLine(CELL_SIZE / 2, BOARD_SIZE * CELL_SIZE - CELL_SIZE / 2,
                 BOARD_SIZE * CELL_SIZE - CELL_SIZE / 2, CELL_SIZE / 2);
    }

    private void Draw_Game_Pieces(Graphics g) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                int x = j * CELL_SIZE + CELL_SIZE / 2;
                int y = i * CELL_SIZE + CELL_SIZE / 2;
                if (Game_Board[i][j] == 1) {
                    g.setColor(Player1_Color);
                    g.fillOval(x - 25, y - 25, 50, 50);
                } else if (Game_Board[i][j] == 2) {
                    g.setColor(Player2_Color);
                    g.fillRect(x - 25, y - 25, 50, 50);
                }
                if (Selected_Piece != null && Selected_Piece.x == i && Selected_Piece.y == j) {
                    g.setColor(Color.BLACK);
                    g.drawRect(x - 30, y - 30, 60, 60);
                }
            }
        }
    }

    private void Draw_Player_Labels(Graphics g) {
        g.setColor(Player1_Color);
        g.fillOval(20, BOARD_SIZE * CELL_SIZE + 20, 30, 30);
        g.setColor(Color.BLACK);
        g.drawString(Player1_Name, 60, BOARD_SIZE * CELL_SIZE + 40);

        g.setColor(Player2_Color);
        g.fillRect(150, BOARD_SIZE * CELL_SIZE + 20, 30, 30);
        g.setColor(Color.BLACK);
        g.drawString(Player2_Name, 190, BOARD_SIZE * CELL_SIZE + 40);
        
        g.drawString(Player1_Name + ": " + Player1_Wins, 20, BOARD_SIZE * CELL_SIZE + 70);
        g.drawString(Player2_Name + ": " + Player2_Wins, 150, BOARD_SIZE * CELL_SIZE + 70);
    }

    private void Draw_Winning_Line(Graphics g) {
        if (Winning_Line.size() == 2) {
            Point p1 = Winning_Line.get(0);
            Point p2 = Winning_Line.get(1);
            int x1 = p1.y * CELL_SIZE + CELL_SIZE / 2;
            int y1 = p1.x * CELL_SIZE + CELL_SIZE / 2;
            int x2 = p2.y * CELL_SIZE + CELL_SIZE / 2;
            int y2 = p2.x * CELL_SIZE + CELL_SIZE / 2;

            g.setColor(Current_Player == 1 ? Player1_Color : Player2_Color);
            ((Graphics2D) g).setStroke(new BasicStroke(5));
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private void Draw_Reset_Button(Graphics g) {
        g.setColor(Color.BLACK);
        g.drawRect(Reset_Button.x, Reset_Button.y, Reset_Button.width, Reset_Button.height);
        g.drawString("Restart", Reset_Button.x + 25, Reset_Button.y + 20);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int x = e.getX(), y = e.getY();

        if (Is_Game_Over && Reset_Button.contains(x, y)) {
            Initialize_Board();
            return;
        }

        if (Is_Game_Over) return;

        int col = x / CELL_SIZE;
        int row = y / CELL_SIZE;

        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) return;

        if (Selected_Piece == null) {
            if (Game_Board[row][col] == Current_Player) {
                Selected_Piece = new Point(row, col);
            }
        } else {
            if (Is_Valid_Move(Selected_Piece.x, Selected_Piece.y, row, col)) {
                Game_Board[row][col] = Current_Player;
                Game_Board[Selected_Piece.x][Selected_Piece.y] = 0;

                if (Current_Player == 1) {
                    Player1_Moved_Pieces.add(new Point(row, col));
                    if (Player1_Moved_Pieces.size() == 3) {
                        Player1_All_Moved = true;
                    }
                } else {
                    Player2_Moved_Pieces.add(new Point(row, col));
                    if (Player2_Moved_Pieces.size() == 3) {
                        Player2_All_Moved = true;
                    }
                }

                if (Check_Win(Current_Player) && (Current_Player == 1 ? Player1_All_Moved : Player2_All_Moved)) {
                    Is_Game_Over = true;
                    if (Current_Player == 1) Player1_Wins++;
                    else Player2_Wins++;
                    Score_Label.setText(Player1_Name + ": " + Player1_Wins + " | " + Player2_Name + ": " + Player2_Wins);
                    JOptionPane.showMessageDialog(this, (Current_Player == 1 ? Player1_Name : Player2_Name) + " wins!");
                }

                if (Is_Online_Game) {
                    Send_Game_Move(Selected_Piece.x, Selected_Piece.y, row, col);
                }
                
                Current_Player = (Current_Player == 1) ? 2 : 1;
            }
            Selected_Piece = null;
        }
        repaint();
    }

    private boolean Is_Valid_Move(int fromRow, int fromCol, int toRow, int toCol) {
        if (Game_Board[toRow][toCol] != 0) return false;
        int dr = Math.abs(fromRow - toRow);
        int dc = Math.abs(fromCol - toCol);
        return (dr <= 1 && dc <= 1);
    }

    private boolean Check_Win(int player) {
        if ((player == 1 && !Player1_All_Moved) || (player == 2 && !Player2_All_Moved)) {
            return false;
        }

        for (int i = 0; i < BOARD_SIZE; i++) {
            if (Game_Board[i][0] == player && Game_Board[i][1] == player && Game_Board[i][2] == player) {
                Winning_Line = new ArrayList<>(Arrays.asList(new Point(i, 0), new Point(i, 2)));
                return true;
            }
            if (Game_Board[0][i] == player && Game_Board[1][i] == player && Game_Board[2][i] == player) {
                Winning_Line = new ArrayList<>(Arrays.asList(new Point(0, i), new Point(2, i)));
                return true;
            }
        }

        if (Game_Board[0][0] == player && Game_Board[1][1] == player && Game_Board[2][2] == player) {
            Winning_Line = new ArrayList<>(Arrays.asList(new Point(0, 0), new Point(2, 2)));
            return true;
        }
        if (Game_Board[0][2] == player && Game_Board[1][1] == player && Game_Board[2][0] == player) {
            Winning_Line = new ArrayList<>(Arrays.asList(new Point(0, 2), new Point(2, 0)));
            return true;
        }

        return false;
    }

    private void Change_Player_Names() {
        Player1_Name = JOptionPane.showInputDialog(this, "Enter Player 1 Name:", Player1_Name);
        Player2_Name = JOptionPane.showInputDialog(this, "Enter Player 2 Name:", Player2_Name);
        if (Player1_Name == null) Player1_Name = "Player 1";
        if (Player2_Name == null) Player2_Name = "Player 2";
        Score_Label.setText(Player1_Name + ": " + Player1_Wins + " | " + Player2_Name + ": " + Player2_Wins);
        repaint();
    }

    private void Change_Player_Colors() {
        Color newColor1 = JColorChooser.showDialog(this, "Choose " + Player1_Name + "'s Color", Player1_Color);
        Color newColor2 = JColorChooser.showDialog(this, "Choose " + Player2_Name + "'s Color", Player2_Color);
        if (newColor1 != null) Player1_Color = newColor1;
        if (newColor2 != null) Player2_Color = newColor2;
        repaint();
    }

    private void Show_Game_Rules() {
        String rules = "Three Men's Morris Rules:\n\n" +
                      "1. Each player has 3 pieces\n" +
                      "2. All pieces must move before winning\n" +
                      "3. Move to adjacent squares (including diagonals)\n" +
                      "4. First to get 3 in a row wins!\n\n" +
                      "Network Play:\n" +
                      "- Host creates game, other player joins\n" +
                      "- Both players must be on same network";
        JOptionPane.showMessageDialog(this, rules, "Game Rules", JOptionPane.INFORMATION_MESSAGE);
    }

    private void Show_About_Dialog() {
        String about = "Three Men's Morris\n" +
                     "Version 2.0\n" +
                     "Developed by Muhammad Mubashir Shafique\n" +
                     "Features:\n" +
                     "- Local & Network Multiplayer\n" +
                     "- Score Tracking\n" +
                     "- Customizable Appearance";
        JOptionPane.showMessageDialog(this, about, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Three_Mens_Morris::new);
    }
}

class Game_Move implements Serializable {
    int From_Row, From_Col, To_Row, To_Col;
    
    public Game_Move(int fromRow, int fromCol, int toRow, int toCol) {
        this.From_Row = fromRow;
        this.From_Col = fromCol;
        this.To_Row = toRow;
        this.To_Col = toCol;
    }
}