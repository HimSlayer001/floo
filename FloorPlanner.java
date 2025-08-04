import javax.swing.*;
    import java.awt.*;
    import java.awt.event.*;
    import java.awt.image.BufferedImage;
    import java.io.*;
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.Map;

    public class FloorPlanner extends JFrame {
        private JPanel canvasPanel;
        private JPanel controlPanel;
        private JButton addRoomButton, addDoorButton, addWindowButton, addFurnitureButton, rotateFurnitureButton, saveButton, loadButton;
        private JComboBox<RoomType> roomTypeComboBox;
        private JComboBox<String> relativePositionComboBox, furnitureTypeComboBox;
        public static ArrayList<Room> rooms;
        private Room selectedRoom;  
        private Point initialClick;
        private Furniture selectedFurniture;
        private int originalX, originalY;
        private JPanel furniturePanel;
        private JPanel fixturePanel;
        private ImageIcon bedIcon, chairIcon, tableIcon, sofaIcon, diningSetIcon;
        private ImageIcon commodeIcon, washbasinIcon, showerIcon, sinkIcon, stoveIcon;
    
        
        public enum FurnitureType {
            BED(80, 120, "bed"),
            CHAIR(40, 40, "chair"),
            TABLE(60, 60, "table"),
            SOFA(120, 60, "sofa"),
            DINING_SET(120, 120, "dining_set"),
            COMMODE(50, 35, "commode"),
            WASHBASIN(40, 30, "washbasin"),
            SHOWER(60, 60, "shower"),
            SINK(50, 30, "sink"),
            STOVE(60, 30, "stove");

            private final int width;
            private final int height;
            private final String imagePath;

            FurnitureType(int width, int height, String imagePath) {
                this.width = width;
                this.height = height;
                this.imagePath = "placeholder_path/" + imagePath + ".png"; // Placeholder path
            }
        }
        public FloorPlanner() {
            setTitle("2D Floor Planner");
            setSize(1000, 600);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setLayout(new BorderLayout());

            rooms = new ArrayList<>();
            initializeComponents();
            loadImages();
            setupLayout();
        }
            // Initialize canvas panel
            private void initializeComponents() {
                
                // Initialize main panels

                canvasPanel = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2d = (Graphics2D) g;
                        drawGrid(g2d);
                        drawRooms(g2d);
                    }
                
                };

            controlPanel = new JPanel();
            
            
            
            canvasPanel.setBackground(Color.LIGHT_GRAY);
            canvasPanel.setPreferredSize(new Dimension(800, 600));
            canvasPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    selectRoom(e.getPoint());
                    selectFurniture(e.getPoint());
                    initialClick = e.getPoint();

                    if (selectedRoom != null) {
                        originalX = selectedRoom.getX();
                        originalY = selectedRoom.getY();
                        selectedRoom.saveOriginalPositions();
                    }

                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (selectedRoom != null) {
                        // Check for overlap at the release position
                        if (isOverlapping(selectedRoom)) {
                            // Show error dialog
                            JOptionPane.showMessageDialog(
                                FloorPlanner.this,
                                "Overlap error",
                                "Overlap Error",
                                JOptionPane.ERROR_MESSAGE
                            );
                            
                            // Snap back to original position
                            selectedRoom.setX(originalX);
                            selectedRoom.setY(originalY);
                            selectedRoom.restoreOriginalPositions();
                            canvasPanel.repaint();
                        }
                    }
                }
            });
            canvasPanel.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (selectedRoom != null) {
                        int deltaX = e.getPoint().x - initialClick.x;
                        int deltaY = e.getPoint().y - initialClick.y;
                        selectedRoom.setX(selectedRoom.getX() + deltaX);
                        selectedRoom.setY(selectedRoom.getY() + deltaY);
                        selectedRoom.moveElements(deltaX, deltaY);  // Add this line
                        initialClick = e.getPoint();
                        canvasPanel.repaint();
                    }
                    if (selectedFurniture != null) {
                        moveFurniture(e.getPoint());
                    }
                }
            });
            
            add(canvasPanel, BorderLayout.CENTER);

            // Initialize control panel
            
            controlPanel.setLayout(new GridLayout(13, 1));
            controlPanel.setPreferredSize(new Dimension(200, 600));

            // Room type selection
            roomTypeComboBox = new JComboBox<>(RoomType.values());
            controlPanel.add(new JLabel("Select Room Type:"));
            controlPanel.add(roomTypeComboBox);

            // Relative position selection
            String[] positions = { "None", "North", "South", "East", "West" };
            relativePositionComboBox = new JComboBox<>(positions);
            controlPanel.add(new JLabel("Relative Position:"));
            controlPanel.add(relativePositionComboBox);

            // Add room button
            addRoomButton = new JButton("Add Room");
            addRoomButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addRoom();
                }
            });
            controlPanel.add(addRoomButton);

            // Add door button
            addDoorButton = new JButton("Add Door");
            addDoorButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addDoor();
                }
            });
            controlPanel.add(addDoorButton);

            // Add window button
            addWindowButton = new JButton("Add Window");
            addWindowButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addWindow();
                }
            });
            controlPanel.add(addWindowButton);

            // Furniture type selection
            String[] furnitureTypes = { "Bed", "Table", "Sofa" };
            furnitureTypeComboBox = new JComboBox<>(furnitureTypes);
            controlPanel.add(new JLabel("Select Furniture Type:"));
            controlPanel.add(furnitureTypeComboBox);

            // Add furniture button
            addFurnitureButton = new JButton("Add Furniture");
            addFurnitureButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addFurniture();
                }
            });
            controlPanel.add(addFurnitureButton);

            // Rotate furniture button
            rotateFurnitureButton = new JButton("Rotate Furniture");
            rotateFurnitureButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    rotateFurniture();
                }
            });
            controlPanel.add(rotateFurnitureButton);

            // Save button
            saveButton = new JButton("Save Plan");
            saveButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    savePlan();
                }
            });
            controlPanel.add(saveButton);

            // Load button
            loadButton = new JButton("Load Plan");
            loadButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    loadPlan();
                }
            });
            controlPanel.add(loadButton);

            add(controlPanel, BorderLayout.WEST);
        }

        private void addRoom() {
            RoomType selectedType = (RoomType) roomTypeComboBox.getSelectedItem();
            String selectedPosition = (String) relativePositionComboBox.getSelectedItem();

        int width = Integer.parseInt(JOptionPane.showInputDialog(this, "Enter room width:"));
        int height = Integer.parseInt(JOptionPane.showInputDialog(this, "Enter room height:"));
            int x = 10;
            int y = 10;

            if (rooms.size() > 0 && !selectedPosition.equals("None")) {
                Room lastRoom = rooms.get(rooms.size() - 1);
                switch (selectedPosition) {
                    case "North":
                        x = lastRoom.getX();
                        y = lastRoom.getY() - height - 10;
                        break;
                    case "South":
                        x = lastRoom.getX();
                        y = lastRoom.getY() + lastRoom.getHeight() + 10;
                        break;
                    case "East":
                        x = lastRoom.getX() + lastRoom.getWidth() + 10;
                        y = lastRoom.getY();
                        break;
                    case "West":
                        x = lastRoom.getX() - width - 10;
                        y = lastRoom.getY();
                        break;
                }
            } else {
                x = (rooms.size() % 6) * (width + 10);
                y = (rooms.size() / 6) * (height + 10);
            }

            Room newRoom = new Room(x, y, width, height, selectedType);
            if (!isOverlapping(newRoom)) {
                rooms.add(newRoom);
                canvasPanel.repaint();
            } else {
                JOptionPane.showMessageDialog(this, "Room cannot overlap with existing rooms.");
            }
        }

        private void setupLayout() {
            // Main layout
            furniturePanel = new JPanel();
            fixturePanel = new JPanel();
            JPanel rightPanel = new JPanel(new BorderLayout());
            rightPanel.setPreferredSize(new Dimension(300, 600));
            
            // Control panel setup
            controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
            
            // Furniture panel setup
            furniturePanel.setBorder(BorderFactory.createTitledBorder("Furniture"));
            furniturePanel.setLayout(new GridLayout(3, 2, 5, 5));
            addFurnitureButtons();
            
            // Fixture panel setup
            fixturePanel.setBorder(BorderFactory.createTitledBorder("Fixtures"));
            fixturePanel.setLayout(new GridLayout(3, 2, 5, 5));
            addFixtureButtons();
            
            // Combine panels
            rightPanel.add(controlPanel, BorderLayout.NORTH);
            rightPanel.add(furniturePanel, BorderLayout.CENTER);
            rightPanel.add(fixturePanel, BorderLayout.SOUTH);
            
            add(canvasPanel, BorderLayout.CENTER);
            add(rightPanel, BorderLayout.EAST);
        }

        private void addFurnitureButtons() {
            JButton bedBtn = new JButton(bedIcon);
            bedBtn.setActionCommand("BED");
            JButton chairBtn = new JButton(chairIcon);
            chairBtn.setActionCommand("CHAIR");
            // Add other furniture buttons
            
            // Add buttons to panel
            furniturePanel.add(bedBtn);
            furniturePanel.add(chairBtn);
            // Add other buttons
        }

        private void addFixtureButtons() {
            // Similar to addFurnitureButtons but for fixtures
        }

        private void loadImages() {
            // Initialize image icons with placeholders
            // Replace "path_to_images/" with actual path when implementing
            bedIcon = createImageIcon("Images\\Screenshot 2024-11-16 192445.png");
            chairIcon = createImageIcon("placeholder_path/chair.png");
            tableIcon = createImageIcon("placeholder_path/table.png");
            sofaIcon = createImageIcon("placeholder_path/sofa.png");
            diningSetIcon = createImageIcon("placeholder_path/dining_set.png");
            commodeIcon = createImageIcon("placeholder_path/commode.png");
            washbasinIcon = createImageIcon("placeholder_path/washbasin.png");
            showerIcon = createImageIcon("placeholder_path/shower.png");
            sinkIcon = createImageIcon("placeholder_path/sink.png");
            stoveIcon = createImageIcon("placeholder_path/stove.png");
        }

        private ImageIcon createImageIcon(String path) {
            File file = new File(path);
            if (file.exists()) {
                System.out.println("Loaded image: " + file.getAbsolutePath());
                return new ImageIcon(path);
            } else {
                System.err.println("Image not found: " + file.getAbsolutePath());
                return null;
            }
        }
        private void addDoor() {
            if (selectedRoom != null) {
                Door door = new Door(selectedRoom.getX() + selectedRoom.getWidth() / 2, selectedRoom.getY(), 20, 10);
        
                // Check for overlaps and wall validity
                if (selectedRoom.canAddDoor(door, rooms)) {
                    selectedRoom.addDoor(door);
                    canvasPanel.repaint();
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Cannot place door here. Ensure it is on a wall and does not overlap.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a room to add a door.");
            }
        }
        
        private void addWindow() {
            if (selectedRoom != null) {
                Window window = new Window(selectedRoom.getX(), selectedRoom.getY() + selectedRoom.getHeight() / 2, 40, 10);
        
                // Check for overlaps and wall validity
                if (selectedRoom.canAddWindow(window, rooms)) {
                    selectedRoom.addWindow(window);
                    canvasPanel.repaint();
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Cannot place window here. Ensure it is on an external wall and does not overlap.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a room to add a window.");
            }
        }
        
        
        private void addFurniture() {
            if (selectedRoom != null) {
                // Get the selected furniture type from the combo box
                String selectedFurnitureType = (String) furnitureTypeComboBox.getSelectedItem();
        
                // Determine the initial position for the new furniture
                int x = selectedRoom.getX() + 10;
                int y = selectedRoom.getY() + 10;
        
                // Check for available space to place the furniture without overlap
                boolean foundPosition = false;
                while (!foundPosition) {
                    Furniture newFurniture = new Furniture(x, y, selectedFurnitureType);
        
                    if (!selectedRoom.isOverlapping(newFurniture)) {
                        // Add the furniture if there is no overlap
                        selectedRoom.addFurniture(newFurniture);
                        canvasPanel.repaint();
                        foundPosition = true; // Exit the loop
                    } else {
                        // Move the position slightly to find the next available spot
                        x += 50; // Adjust horizontal spacing
                        if (x + 40 > selectedRoom.getX() + selectedRoom.getWidth()) {
                            // Move to the next row if exceeding room width
                            x = selectedRoom.getX() + 10;
                            y += 50; // Adjust vertical spacing
                        }
                        // Check if we're out of space in the room
                        if (y + 40 > selectedRoom.getY() + selectedRoom.getHeight()) {
                            JOptionPane.showMessageDialog(this, "Not enough space to add furniture in this room.");
                            return;
                        }
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a room first.");
        }
    }

        private void rotateFurniture() {
            if (selectedFurniture != null) {
                selectedFurniture.rotate();
                if (!selectedRoom.isOverlapping(selectedFurniture)) {
                    canvasPanel.repaint();
                } else {
                    selectedFurniture.rotateBack();
                    JOptionPane.showMessageDialog(this,
                            "Cannot rotate furniture here, it overlaps with existing elements.");
                }
                
            }
            else{
                JOptionPane.showMessageDialog(this, "Please select a piece of furniture to rotate.");
            }
        }

        private boolean isOverlapping(Room checkRoom) {
            for (Room room : rooms) {
                if (checkRoom != room && checkRoom.getBounds().intersects(room.getBounds())) {
                    return true;
                }
            }
            return false;
        }

        private void drawGrid(Graphics g) {
            g.setColor(Color.GRAY);
            for (int i = 0; i < getWidth(); i += 20) {
                g.drawLine(i, 0, i, getHeight());
            }
            for (int j = 0; j < getHeight(); j += 20) {
                g.drawLine(0, j, getWidth(), j);
            }
        }

        private void drawRooms(Graphics2D g2d) {
            for (Room room : rooms) {
                // Draw room
                room.drawWalls(g2d, rooms);

                g2d.setColor(room.getColor());
                g2d.fillRect(room.getX(), room.getY(), room.getWidth(), room.getHeight());
                g2d.setColor(Color.BLACK);
                g2d.drawRect(room.getX(), room.getY(), room.getWidth(), room.getHeight());
                
                // Draw doors (as wall openings)
                for (Door door : room.getDoors()) {
                    drawDoor(g2d, door, room);
                }
                
                // Draw windows (as dashed lines)
                g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 
                    0, new float[]{5}, 0));
                for (Window window : room.getWindows()) {
                    drawWindow(g2d, window, room);
                }
                
                // Reset stroke
                g2d.setStroke(new BasicStroke(1));
                
                // Draw furniture
                for (Furniture furniture : room.getFurnitures()) {
                    drawFurniture(g2d, furniture);
                }
            }
        }

        private void drawDoor(Graphics2D g2d, Door door, Room room) {
            g2d.setColor(Color.LIGHT_GRAY); // Same color as external walls for door opening
            g2d.fillRect(door.getX(), door.getY(), door.getWidth(), door.getHeight());
        }
        
        private void drawWindow(Graphics2D g2d, Window window, Room room) {
            g2d.setColor(Color.BLUE); // Windows are dashed lines
            Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10}, 0);
            g2d.setStroke(dashed);
        
            // Draw dashed line representing the window
            g2d.drawLine(window.getX(), window.getY(),
                         window.getX() + window.getWidth(), window.getY());
        
            g2d.setStroke(new BasicStroke()); // Reset stroke
        }
        
        
    private void drawFurniture(Graphics2D g2d, Furniture furniture) {
        // Draw furniture with image if available, otherwise draw placeholder
        ImageIcon icon = getIconForFurniture(furniture.getType());
        if (icon != null) {
            g2d.drawImage(icon.getImage(), furniture.getX(), furniture.getY(), 
                        furniture.getWidth(), furniture.getHeight(), null);
        } else {
            g2d.setColor(Color.GRAY);
            g2d.fillRect(furniture.getX(), furniture.getY(), 
                        furniture.getWidth(), furniture.getHeight());
        }
    }

    private ImageIcon getIconForFurniture(String type) {
        // Return appropriate icon based on furniture type
        switch (type.toUpperCase()) {
            case "BED": return bedIcon;
            case "CHAIR": return chairIcon;
            case "TABLE": return tableIcon;
            case "SOFA": return sofaIcon;
            case "DINING_SET": return diningSetIcon;
            case "COMMODE": return commodeIcon;
            case "WASHBASIN": return washbasinIcon;
            case "SHOWER": return showerIcon;
            case "SINK": return sinkIcon;
            case "STOVE": return stoveIcon;
            default: return null;
        }
    }

    private boolean canAddDoor(Room room, Door door) {
        // Check if door is on wall and not overlapping with windows
        // Also check bedroom/bathroom restriction
        if (room.getType() == RoomType.BEDROOM || room.getType() == RoomType.BATHROOM) {
            // Check if door leads to outside
            boolean connectsToRoom = false;
            for (Room otherRoom : rooms) {
                if (otherRoom != room && doorConnectsRooms(door, room, otherRoom)) {
                    connectsToRoom = true;
                    break;
                }
            }
            if (!connectsToRoom) return false;
        }
        return !room.isOverlapping(door);
    }
    private boolean canAddWindow(Room room, Window window) {
        // Check if window is on external wall and not between rooms
        for (Room otherRoom : rooms) {
            if (otherRoom != room && windowBetweenRooms(window, room, otherRoom)) {
                return false;
            }
        }
        return !room.isOverlapping(window);
    }
    private boolean doorConnectsRooms(Door door, Room room1, Room room2) {
        // Check if door connects two rooms
        Rectangle doorBounds = door.getBounds();
        Rectangle room1Bounds = room1.getBounds();
        Rectangle room2Bounds = room2.getBounds();
        
        return doorBounds.intersects(room1Bounds) && doorBounds.intersects(room2Bounds);
    }

    private boolean windowBetweenRooms(Window window, Room room1, Room room2) {
        // Check if window is between two rooms
        Rectangle windowBounds = window.getBounds();
        Rectangle room1Bounds = room1.getBounds();
        Rectangle room2Bounds = room2.getBounds();
        
        return windowBounds.intersects(room1Bounds) && windowBounds.intersects(room2Bounds);
    }

        private void savePlan() {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Floor Plan");
            int userSelection = fileChooser.showSaveDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();

                try (FileOutputStream fos = new FileOutputStream(fileToSave);
                        ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                    oos.writeObject(rooms);
                    JOptionPane.showMessageDialog(this, "Plan saved successfully.");
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error saving plan: " + e.getMessage());
                }
            }
        }

        /**
         * Load a floor plan from a file.
         * Updates the canvas with loaded rooms and displays a confirmation dialog upon
         * successful load.
         */
        private void loadPlan() {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Load Floor Plan");
            int userSelection = fileChooser.showOpenDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToLoad = fileChooser.getSelectedFile();

                try (FileInputStream fis = new FileInputStream(fileToLoad);
                        ObjectInputStream ois = new ObjectInputStream(fis)) {
                    rooms = (ArrayList<Room>) ois.readObject();
                    canvasPanel.repaint();
                    JOptionPane.showMessageDialog(this, "Plan loaded successfully.");
                } catch (IOException | ClassNotFoundException e) {
                    JOptionPane.showMessageDialog(this, "Error loading plan: " + e.getMessage());
                }
            }
        }

        private void selectRoom(Point clickPoint) {
            for (Room room : rooms) {
                if (room.getBounds().contains(clickPoint)) {
                    selectedRoom = room;
                    selectedFurniture = null;
                    break;
                }
            }
        }

        private void selectFurniture(Point clickPoint) {
            if (selectedRoom != null) {
                for (Furniture furniture : selectedRoom.getFurnitures()) {
                    if (furniture.getBounds().contains(clickPoint)) {
                        selectedFurniture = furniture;
                        break;
                    }
                }
            }
        }

        private void moveRoom(Point newPoint) {
            int deltaX = newPoint.x - initialClick.x;
            int deltaY = newPoint.y - initialClick.y;
            selectedRoom.setX(selectedRoom.getX() + deltaX);
            selectedRoom.setY(selectedRoom.getY() + deltaY);
            initialClick = newPoint;

            selectedRoom.moveElements(deltaX, deltaY);

            if (isOverlapping(selectedRoom)) {
                selectedRoom.setX(selectedRoom.getX() - deltaX);
                selectedRoom.setY(selectedRoom.getY() - deltaY);
                selectedRoom.moveElements(-deltaX, -deltaY); 
                JOptionPane.showMessageDialog(this, "Cannot place room here, it overlaps with another room.");
            }

            canvasPanel.repaint();
        }

        private void moveFurniture(Point newPoint) {
            int deltaX = newPoint.x - initialClick.x;
            int deltaY = newPoint.y - initialClick.y;
            selectedFurniture.setX(selectedFurniture.getX() + deltaX);
            selectedFurniture.setY(selectedFurniture.getY() + deltaY);
            initialClick = newPoint;

            if (selectedRoom.isOverlapping(selectedFurniture)) {
                selectedFurniture.setX(selectedFurniture.getX() - deltaX);
                selectedFurniture.setY(selectedFurniture.getY() - deltaY);
                JOptionPane.showMessageDialog(this, "Cannot place furniture here, it overlaps with another element.");
            }

            canvasPanel.repaint();
        }

        public static void main(String[] args) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    new FloorPlanner().setVisible(true);
                }
            });
        }
    }

    enum RoomType {
        BEDROOM(Color.GREEN),
        BATHROOM(Color.BLUE),
        KITCHEN(Color.RED),
        DRAWINGROOM(Color.YELLOW);

        private final Color color;

        RoomType(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }
    }

    class Room implements Serializable {
        private static final long serialVersionUID = 1L;
        private int x, y, width, height;
        private RoomType type;
        private ArrayList<Door> doors;
        private ArrayList<Window> windows;
        private ArrayList<Furniture> furnitures;
        private Map<Door, Point> originalDoorPositions = new HashMap<>();
        private Map<Window, Point> originalWindowPositions = new HashMap<>();
        private Map<Furniture, Point> originalFurniturePositions = new HashMap<>();

        public Room(int x, int y, int width, int height, RoomType type) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.type = type;
            this.doors = new ArrayList<>();
            this.windows = new ArrayList<>();
            this.furnitures = new ArrayList<>();

        }
       
        public void draw(Graphics2D g2d) {
            // Draw room walls
            g2d.setColor(Color.BLACK);
    
            // Top wall
            g2d.drawLine(x, y, x + width, y);
    
            // Right wall
            g2d.drawLine(x + width, y, x + width, y + height);
    
            // Bottom wall
            g2d.drawLine(x, y + height, x + width, y + height);
    
            // Left wall
            g2d.drawLine(x, y, x, y + height);
    
            // Draw doors as openings in the wall
            for (Door door : doors) {
                g2d.setColor(Color.WHITE);
                g2d.fillRect(door.getX(), door.getY(), door.getWidth(), door.getHeight());
            }
    
            // Draw windows as dashed lines
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10}, 0));
            for (Window window : windows) {
                g2d.drawLine(window.getX(), window.getY(), window.getX() + window.getWidth(), window.getY());
            }
            g2d.setStroke(new BasicStroke()); // Reset stroke
        }

        private int wallThickness = 10; // Thickness of the walls

        public void drawWalls(Graphics2D g2d, ArrayList<Room> rooms) {
            g2d.setColor(Color.DARK_GRAY); // Default wall color for outside walls
        
            // Check if each wall is shared with another room
            boolean hasNorthWall = true;
            boolean hasSouthWall = true;
            boolean hasEastWall = true;
            boolean hasWestWall = true;
        
            for (Room otherRoom : rooms) {
                if (otherRoom != this) {
                    // Check if this room shares a north wall
                    if (otherRoom.getBounds().intersects(new Rectangle(x, y - 1, width, 1))) {
                        hasNorthWall = false;
                        g2d.setColor(Color.BLACK);
                        g2d.fillRect(x, y, width, 10); // Draw shared north wall
                    }
                    // Check if this room shares a south wall
                    if (otherRoom.getBounds().intersects(new Rectangle(x, y + height, width, 1))) {
                        hasSouthWall = false;
                        g2d.setColor(Color.BLACK);
                        g2d.fillRect(x, y + height - 10, width, 10); // Draw shared south wall
                    }
                    // Check if this room shares an east wall
                    if (otherRoom.getBounds().intersects(new Rectangle(x + width, y, 1, height))) {
                        hasEastWall = false;
                        g2d.setColor(Color.BLACK);
                        g2d.fillRect(x + width - 10, y, 10, height); // Draw shared east wall
                    }
                    // Check if this room shares a west wall
                    if (otherRoom.getBounds().intersects(new Rectangle(x - 1, y, 1, height))) {
                        hasWestWall = false;
                        g2d.setColor(Color.BLACK);
                        g2d.fillRect(x, y, 10, height); // Draw shared west wall
                    }
                }
            }
        
            // Draw external walls if not shared
            if (hasNorthWall) {
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.fillRect(x, y, width, 10); // Top wall
            }
            if (hasSouthWall) {
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.fillRect(x, y + height - 10, width, 10); // Bottom wall
            }
            if (hasEastWall) {
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.fillRect(x + width - 10, y, 10, height); // Right wall
            }
            if (hasWestWall) {
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.fillRect(x, y, 10, height); // Left wall
            }
        }
        
        
        public void saveOriginalPositions() {
        originalDoorPositions.clear();
        originalWindowPositions.clear();
        originalFurniturePositions.clear();
        
        for (Door door : doors) {
            originalDoorPositions.put(door, new Point(door.getX(), door.getY()));
        }
        
        for (Window window : windows) {
            originalWindowPositions.put(window, new Point(window.getX(), window.getY()));
        }
        
        for (Furniture furniture : furnitures) {
            originalFurniturePositions.put(furniture, new Point(furniture.getX(), furniture.getY()));
        }
    }

    public void restoreOriginalPositions() {
        for (Door door : doors) {
            Point original = originalDoorPositions.get(door);
            if (original != null) {
                door.setX(original.x);
                door.setY(original.y);
            }
        }
        
        for (Window window : windows) {
            Point original = originalWindowPositions.get(window);
            if (original != null) {
                window.setX(original.x);
                window.setY(original.y);
            }
        }
        
        for (Furniture furniture : furnitures) {
            Point original = originalFurniturePositions.get(furniture);
            if (original != null) {
                furniture.setX(original.x);
                furniture.setY(original.y);
            }
        }
    }
    public boolean canAddDoor(Door door, ArrayList<Room> rooms) {
        boolean onWall = (door.getX() == x || door.getX() == x + width - door.getWidth() ||
                         door.getY() == y || door.getY() == y + height - door.getHeight());
    
        if (type == RoomType.BEDROOM || type == RoomType.BATHROOM) {
            // Ensure doors do not lead outside
            boolean connectsToAnotherRoom = connectsToAnotherRoom(door, rooms);
            return onWall && connectsToAnotherRoom && !isOverlapping(door);
        }
    
        return onWall && !isOverlapping(door);
    }
    
    private boolean connectsToAnotherRoom(Door door, ArrayList<Room> rooms) {
        for (Room otherRoom : rooms) {
            if (otherRoom != this && door.getBounds().intersects(otherRoom.getBounds())) {
                return true;
            }
        }
        return false;
    }
    
        public boolean canAddWindow(Window window) {
            // Check if window is on wall
            boolean onWall = (window.getX() == x || window.getX() == x + width - window.getWidth() ||
                             window.getY() == y || window.getY() == y + height - window.getHeight());
            
            return onWall && !isOverlapping(window);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public Color getColor() {
            return type.getColor();
        }

        public ArrayList<Door> getDoors() {
            return doors;
        }

        public ArrayList<Window> getWindows() {
            return windows;
        }

        public ArrayList<Furniture> getFurnitures() {
            return furnitures;
        }

        public void setX(int x) {
            this.x = x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public void addDoor(Door door) {
            doors.add(door);
        }

        public void addWindow(Window window) {
            windows.add(window);
        }

        public void addFurniture(Furniture furniture) {
            furnitures.add(furniture);
        }

        public void moveElements(int deltaX, int deltaY) {
        for (Door door : doors) {
            door.setX(door.getX() + deltaX);
            door.setY(door.getY() + deltaY);
        }
        for (Window window : windows) {
            window.setX(window.getX() + deltaX);
            window.setY(window.getY() + deltaY);
        }
        for (Furniture furniture : furnitures) {
            furniture.setX(furniture.getX() + deltaX);
            furniture.setY(furniture.getY() + deltaY);
        }
    }


        public boolean isOverlapping(Door door) {
            Rectangle doorBounds = new Rectangle(door.getX(), door.getY(), door.getWidth(), door.getHeight());
            for (Door existingDoor : doors) {
                if (doorBounds.intersects(existingDoor.getBounds())) {
                    return true;
                }
            }
            for (Window window : windows) {
                if (doorBounds.intersects(window.getBounds())) {
                    return true;
                }
            }
            for (Furniture furniture : furnitures) {
                if (doorBounds.intersects(furniture.getBounds())) {
                    return true;
                }
            }
            return false;
        }

        public boolean isOverlapping(Window window) {
            Rectangle windowBounds = new Rectangle(window.getX(), window.getY(), window.getWidth(), window.getHeight());
            for (Door door : doors) {
                if (windowBounds.intersects(door.getBounds())) {
                    return true;
                }
            }
            for (Window existingWindow : windows) {
                if (windowBounds.intersects(existingWindow.getBounds())) {
                    return true;
                }
            }
            for (Furniture furniture : furnitures) {
                if (windowBounds.intersects(furniture.getBounds())) {
                    return true;
                }
            }
            return false;
        }

        public boolean isOverlapping(Furniture furniture) {
            Rectangle furnitureBounds = new Rectangle(furniture.getX(), furniture.getY(),
                                                      furniture.getWidth(), furniture.getHeight());
        
            for (Door door : doors) {
                if (furnitureBounds.intersects(door.getBounds())) {
                    return true;
                }
            }
            for (Window window : windows) {
                if (furnitureBounds.intersects(window.getBounds())) {
                    return true;
                }
            }
            for (Furniture existingFurniture : furnitures) {
                if (furniture != existingFurniture && furnitureBounds.intersects(existingFurniture.getBounds())) {
                    return true;
                }
            }
            return false;
        }
        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

    RoomType getType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    boolean canAddWindow(Window newWindow, ArrayList<Room> rooms) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    }

    class Door implements Serializable {
        private static final long serialVersionUID = 1L;
        private int x, y, width, height;

        public void setX(int x) {
            this.x = x;
        }

        public void setY(int y) {
            this.y = y;
        }


        public Door(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    class Window implements Serializable {
        private static final long serialVersionUID = 1L;
        private int x, y, width, height;

        public Window(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public void setX(int x) {
            this.x = x;
        }

        public void setY(int y) {
            this.y = y;
        }


        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    class Furniture implements Serializable {
        private static final long serialVersionUID = 1L;
        private int x, y, width, height;
        private String type;
        private int rotation;
        private transient Image image; // Not serialized
        private String imagePath; // Path to image file

        public Furniture(int x, int y, String type) {
            this.x = x;
            this.y = y;
            this.width = 40;
            this.height = 40;
            this.type = type;
            this.rotation = 0;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public String getType() {
            return type;
        }

        public void setX(int x) {
            this.x = x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public void rotate() {
            int temp = width;
            width = height;
            height = temp;
            rotation = (rotation + 90) % 360;
        }

        public void rotateBack() {
            rotation = (rotation - 90 + 360) % 360; // Reverse the rotation
            int temp = width;
            width = height;
            height = temp;
        }
    
        public int getRotation() {
            return rotation;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }