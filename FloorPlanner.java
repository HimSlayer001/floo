import javax.swing.*;
    import java.awt.*;
    import java.awt.event.*;
    import java.io.*;
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.Map;
    import java.awt.geom.AffineTransform;
    import javax.swing.filechooser.FileNameExtensionFilter;

    public class FloorPlanner extends JFrame {
        private JPanel canvasPanel;
        private JPanel controlPanel;
        private JButton addRoomButton, addDoorButton, addWindowButton, addFurnitureButton, rotateFurnitureButton,deleteRoomButton;
        private JComboBox<RoomType> roomTypeComboBox;
        private JComboBox<String> relativePositionComboBox, furnitureTypeComboBox;
        private JComboBox<String> fixtureTypeComboBox;
        private JToolBar topToolBar;
        private JButton newFrameButton, saveButton, loadButton;
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

        private boolean canAddWindow(Room room, Window window, ArrayList<Room> rooms) {
            // Check if window overlaps with any other room
            Rectangle windowBounds = window.getBounds();
            windowBounds.grow(5, 5); // Add small margin for checking

            for (Room otherRoom : rooms) {
                if (otherRoom != room && otherRoom.getBounds().intersects(windowBounds)) {
                    return false;
                }
            }

            // Check if window overlaps with existing windows or doors in the room
            return room.canAddWindow(window);
        }
        public FloorPlanner() {
            setTitle("2D Floor Planner");
            setSize(1000, 600);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setLayout(new BorderLayout());

            rooms = new ArrayList<>();
            initializeToolBar();
            initializeComponents();
            loadImages();
            setupLayout();
        }
        private void rotateRoom() {
            if (selectedRoom != null) {
                // Swap width and height
                int tempWidth = selectedRoom.getWidth();
                int tempHeight = selectedRoom.getHeight();
                
                // Update room dimensions
                try {
                    // Reflection to modify private fields
                    java.lang.reflect.Field widthField = Room.class.getDeclaredField("width");
                    java.lang.reflect.Field heightField = Room.class.getDeclaredField("height");
                    
                    widthField.setAccessible(true);
                    heightField.setAccessible(true);
                    
                    widthField.set(selectedRoom, tempHeight);
                    heightField.set(selectedRoom, tempWidth);
                    
                    // Check if rotation causes overlap
                    if (isOverlapping(selectedRoom)) {
                        // Revert if overlap occurs
                        widthField.set(selectedRoom, tempWidth);
                        heightField.set(selectedRoom, tempHeight);
                        
                        JOptionPane.showMessageDialog(this, 
                            "Cannot rotate room. It would overlap with another room.",
                            "Rotation Error", 
                            JOptionPane.ERROR_MESSAGE);
                    } else {
                        // Adjust rooms' doors, windows, and furniture during rotation
                        adjustElementsAfterRotation(selectedRoom, tempWidth, tempHeight);
                        
                        canvasPanel.repaint();
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, 
                        "Error rotating room: " + e.getMessage(), 
                        "Rotation Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Please select a room to rotate.", 
                    "No Room Selected", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
        
        private void adjustElementsAfterRotation(Room room, int oldWidth, int oldHeight) {
            // Adjust doors, windows, and furniture positions based on rotation
            for (Door door : room.getDoors()) {
                if (door.getX() == room.getX()) {
                    // Left side door
                    door.setX(room.getX() + room.getWidth() - door.getWidth());
                } else if (door.getX() == room.getX() + oldWidth - door.getWidth()) {
                    // Right side door
                    door.setX(room.getX());
                }
                
                if (door.getY() == room.getY()) {
                    // Top side door
                    door.setY(room.getY() + room.getHeight() - door.getHeight());
                } else if (door.getY() == room.getY() + oldHeight - door.getHeight()) {
                    // Bottom side door
                    door.setY(room.getY());
                }
            }
            
            // Similar adjustments for windows
            for (Window window : room.getWindows()) {
                if (window.getX() == room.getX()) {
                    window.setX(room.getX() + room.getWidth() - window.getWidth());
                } else if (window.getX() == room.getX() + oldWidth - window.getWidth()) {
                    window.setX(room.getX());
                }
                
                if (window.getY() == room.getY()) {
                    window.setY(room.getY() + room.getHeight() - window.getHeight());
                } else if (window.getY() == room.getY() + oldHeight - window.getHeight()) {
                    window.setY(room.getY());
                }
            }
            
            // Adjust furniture positions (simple repositioning)
            for (Furniture furniture : room.getFurnitures()) {
                // Basic repositioning - you might want to improve this logic
                int relativeX = furniture.getX() - room.getX();
                int relativeY = furniture.getY() - room.getY();
                
                furniture.setX(room.getX() + (oldHeight - relativeY - furniture.getHeight()));
                furniture.setY(room.getY() + relativeX);
                
                // Optional: rotate furniture
                furniture.rotate();
            }
        }

        private void initializeToolBar() {
            // Create toolbar
            topToolBar = new JToolBar();
            topToolBar.setFloatable(false); // Prevent toolbar from being dragged
            topToolBar.setRollover(true);
    
            // New Frame button
            newFrameButton = new JButton("New Frame", new ImageIcon("path/to/new-icon.png")); // Replace with actual icon path
            newFrameButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    createNewFrame();
                }
            });

            saveButton = new JButton("Save Plan");
            saveButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    savePlan();
                }
            });

            loadButton = new JButton("Load Plan");
            loadButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    loadPlan();
                }
            });

            
            topToolBar.add(newFrameButton);
            topToolBar.addSeparator();
            topToolBar.add(saveButton);
            topToolBar.add(loadButton);
            add(topToolBar, BorderLayout.NORTH);
        }    
        
        private void createNewFrame() {
            // Prompt user to confirm creating a new frame
            int result = JOptionPane.showConfirmDialog(
                this, 
                "Are you sure you want to create a new floor plan? Unsaved changes will be lost.", 
                "New Floor Plan", 
                JOptionPane.YES_NO_OPTION
            );
            
            if (result == JOptionPane.YES_OPTION) {
                // Clear existing rooms
                rooms.clear();
                
                // Repaint the canvas to show empty floor plan
                canvasPanel.repaint();
            }
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
            // Replace the existing MouseListener in initializeComponents() with this:
canvasPanel.addMouseListener(new MouseAdapter() {
    @Override
    public void mousePressed(MouseEvent e) {
        initialClick = e.getPoint();
        
        // First try to select furniture
        selectedFurniture = null;
        selectedRoom = null;  // Reset room selection first
        
        // Check each room for furniture
        for (Room room : rooms) {
            for (Furniture furniture : room.getFurnitures()) {
                if (furniture.getBounds().contains(e.getPoint())) {
                    selectedFurniture = furniture;
                    selectedRoom = room;  // Set the room when furniture is selected
                    originalX = furniture.getX();
                    originalY = furniture.getY();
                    return;  // Exit after finding furniture
                }
            }
        }
        
        // If no furniture was selected, try to select a room
        if (selectedFurniture == null) {
            for (Room room : rooms) {
                if (room.getBounds().contains(e.getPoint())) {
                    selectedRoom = room;
                    originalX = selectedRoom.getX();
                    originalY = selectedRoom.getY();
                    selectedRoom.saveOriginalPositions();
                    break;
                }
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

        if (selectedRoom != null) {
        // Check for room overlap
            if (isOverlapping(selectedRoom)) {
                // Show error message
                JOptionPane.showMessageDialog(
                    FloorPlanner.this,
                    "Room cannot overlap with other rooms!",
                    "Overlap Error",
                    JOptionPane.ERROR_MESSAGE
                );
                
                // Snap back to original position
                selectedRoom.setX(originalX);
                selectedRoom.setY(originalY);
                selectedRoom.restoreOriginalPositions();
                canvasPanel.repaint();
            }
            // selectedRoom = null; // Reset selection after moving
        }
        if (selectedFurniture != null) {
            // Check if the furniture is still within the room bounds
            if (!selectedRoom.getBounds().contains(selectedFurniture.getBounds())) {
                // Move furniture back to its last valid position
                selectedFurniture.setX(originalX);
                selectedFurniture.setY(originalY);
                canvasPanel.repaint();
            }
            // Don't reset selectedFurniture here to maintain selection
        } else if (selectedRoom != null) {
            if (isOverlapping(selectedRoom)) {
                selectedRoom.setX(originalX);
                selectedRoom.setY(originalY);
                selectedRoom.restoreOriginalPositions();
                canvasPanel.repaint();
            }
        }
    }
});
// Replace the existing MouseMotionListener in initializeComponents() with this:
canvasPanel.addMouseMotionListener(new MouseMotionAdapter() {
    @Override
    public void mouseDragged(MouseEvent e) {

        if (selectedFurniture != null) {
            int deltaX = e.getPoint().x - initialClick.x;
            int deltaY = e.getPoint().y - initialClick.y;
            
            // Store original position before moving
            int oldX = selectedFurniture.getX();
            int oldY = selectedFurniture.getY();
            
            // Try to move the furniture
            selectedFurniture.setX(oldX + deltaX);
            selectedFurniture.setY(oldY + deltaY);
            
            // Check if new position is valid
            boolean isValid = true;
            
            // Check if furniture is still within room bounds
            if (!selectedRoom.getBounds().contains(selectedFurniture.getBounds())) {
                isValid = false;
            }
            
            // Check for overlap with other furniture
            if (isValid) {
                for (Furniture other : selectedRoom.getFurnitures()) {
                    if (other != selectedFurniture && 
                        selectedFurniture.getBounds().intersects(other.getBounds())) {
                        isValid = false;
                        break;
                    }
                }
            }
            
            // If position is invalid, revert to old position
            if (!isValid) {
                selectedFurniture.setX(oldX);
                selectedFurniture.setY(oldY);
            }
            
            initialClick = e.getPoint();
            canvasPanel.repaint();
        } else if (selectedRoom != null) {
            int deltaX = e.getPoint().x - initialClick.x;
            int deltaY = e.getPoint().y - initialClick.y;
            selectedRoom.setX(selectedRoom.getX() + deltaX);
            selectedRoom.setY(selectedRoom.getY() + deltaY);
            selectedRoom.moveElements(deltaX, deltaY);
            initialClick = e.getPoint();
            canvasPanel.repaint();
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

            deleteRoomButton = new JButton("Delete Room");
            deleteRoomButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    deleteSelectedRoom();
                }
            });


            // Add room button
            addRoomButton = new JButton("Add Room");
            addRoomButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addRoom();
                }
            });
            

            addDoorButton = new JButton("Add Door");
            addDoorButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (selectedRoom != null) {
                        addDoor();
                    } else {
                        JOptionPane.showMessageDialog(FloorPlanner.this, 
                            "Please select a room first.", 
                            "No Room Selected", 
                            JOptionPane.WARNING_MESSAGE);
                    }
                }
            });
            

            // Add window button
            addWindowButton = new JButton("Add Window");
            addWindowButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addWindow();
                }
            });

            JButton rotateRoomButton = new JButton("Rotate Room");
            rotateRoomButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            rotateRoom();
            }
        });
            
            controlPanel.add(addRoomButton);
            controlPanel.add(rotateRoomButton);
            controlPanel.add(addDoorButton);
            controlPanel.add(addWindowButton);
            controlPanel.add(deleteRoomButton);


            // Furniture type selection
            String[] furnitureTypes = { "Bed", "Table", "Chair", "Sofa", "Dining Set"};
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
            
            String[] fixtureTypes = { "Commode", "Washbasin", "Shower", "Kitchen Sink", "Stove"};
            fixtureTypeComboBox = new JComboBox<>(fixtureTypes);
            controlPanel.add(new JLabel("Select Fixture Type:"));
            controlPanel.add(fixtureTypeComboBox);

            JButton addFixtureButton = new JButton("Add Fixture");
            addFixtureButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addFixture();
                }
            });
            controlPanel.add(addFixtureButton);

            add(controlPanel, BorderLayout.WEST);
        }

        private void deleteSelectedRoom() {
            if (selectedRoom != null) {
                // Show confirmation dialog
                int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete this room and all its contents?",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                
                if (confirm == JOptionPane.YES_OPTION) {
                    // Check if there are doors connecting to other rooms
                    boolean hasConnectedDoors = false;
                    for (Room otherRoom : rooms) {
                        if (otherRoom != selectedRoom) {
                            for (Door door : selectedRoom.getDoors()) {
                                Rectangle doorBounds = door.getBounds();
                                doorBounds.grow(5, 5); // Add a small margin for better intersection detection
                                if (doorBounds.intersects(otherRoom.getBounds())) {
                                    hasConnectedDoors = true;
                                    break;
                                }
                            }
                            if (hasConnectedDoors) break;
                        }
                    }
                    
                    if (hasConnectedDoors) {
                        // Ask for confirmation if room has connected doors
                        int doorConfirm = JOptionPane.showConfirmDialog(
                            this,
                            "This room has doors connected to other rooms. Deleting it may create invalid door connections.\nDo you still want to proceed?",
                            "Connected Doors Warning",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                        );
                        
                        if (doorConfirm != JOptionPane.YES_OPTION) {
                            return;
                        }
                    }
                    
                    // Remove the room from the list
                    rooms.remove(selectedRoom);
                    
                    // Clear selections
                    selectedRoom = null;
                    selectedFurniture = null;
                    
                    // Repaint the canvas
                    canvasPanel.repaint();
                    
                    // Show success message
                    JOptionPane.showMessageDialog(
                        this,
                        "Room deleted successfully",
                        "Room Deleted",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                }
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Please select a room to delete",
                    "No Room Selected",
                    JOptionPane.WARNING_MESSAGE
                );
            }
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
            
            
            // Combine panels
            rightPanel.add(controlPanel, BorderLayout.NORTH);
            rightPanel.add(furniturePanel, BorderLayout.CENTER);
            rightPanel.add(fixturePanel, BorderLayout.SOUTH);
            
            add(canvasPanel, BorderLayout.CENTER);
            add(rightPanel, BorderLayout.EAST);
        }

        
        private void loadImages() {
            // Initialize image icons with placeholders
            // Replace "path_to_images/" with actual path when implementing
            bedIcon = createImageIcon("furniture/bed.png");
            chairIcon = createImageIcon("furniture/chair.png");
            tableIcon = createImageIcon("furniture/table.png");
            sofaIcon = createImageIcon("furniture/sofa.png");
            diningSetIcon = createImageIcon("furniture/dining.png");
            commodeIcon = createImageIcon("furniture/commode.png");
            washbasinIcon = createImageIcon("furniture/wash basin.png");
            showerIcon = createImageIcon("furniture/shower.png");
            sinkIcon = createImageIcon("furniture/sink.png");
            stoveIcon = createImageIcon("furniture/stove.png");
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


        // Replace the existing addDoor method in FloorPlanner class
private void addDoor() {
    if (selectedRoom != null) {
        // Show dialog to select door position
        String[] options = {"Top Wall", "Bottom Wall", "Left Wall", "Right Wall"};
        int choice = JOptionPane.showOptionDialog(
            this,
            "Select wall for door placement:",
            "Add Door",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );

        Door door = null;
        int doorWidth = 40;  // standard door width
        int doorHeight = selectedRoom.wallThickness;  // door height matches wall thickness

        switch (choice) {
            case 0: // Top Wall
                door = new Door(
                    selectedRoom.getX() + (selectedRoom.getWidth() / 2) - (doorWidth / 2),
                    selectedRoom.getY() - doorHeight/2,
                    doorWidth,
                    doorHeight
                );
                break;
            case 1: // Bottom Wall
                door = new Door(
                    selectedRoom.getX() + (selectedRoom.getWidth() / 2) - (doorWidth / 2),
                    selectedRoom.getY() + selectedRoom.getHeight() - doorHeight/2,
                    doorWidth,
                    doorHeight
                );
                break;
            case 2: // Left Wall
                door = new Door(
                    selectedRoom.getX() - doorHeight/2,
                    selectedRoom.getY() + (selectedRoom.getHeight() / 2) - (doorWidth / 2),
                    doorHeight,
                    doorWidth
                );
                break;
            case 3: // Right Wall
                door = new Door(
                    selectedRoom.getX() + selectedRoom.getWidth() - doorHeight/2,
                    selectedRoom.getY() + (selectedRoom.getHeight() / 2) - (doorWidth / 2),
                    doorHeight,
                    doorWidth
                );
                break;
        }

        if (door != null && selectedRoom.canAddDoor(door, rooms)) {
            selectedRoom.addDoor(door);
            canvasPanel.repaint();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Cannot add door here. Ensure it's on a wall and doesn't overlap with existing elements.",
                "Invalid Door Placement",
                JOptionPane.WARNING_MESSAGE);
        }
    } else {
        JOptionPane.showMessageDialog(this, "Please select a room first.");
    }
}

        private void addWindow() {
    if (selectedRoom != null) {
        // Show dialog to select window position
        String[] options = {"Top Wall", "Bottom Wall", "Left Wall", "Right Wall"};
        int choice = JOptionPane.showOptionDialog(
            this,
            "Select wall for window placement:",
            "Add Window",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );

        Window window = null;
        int windowWidth = 40;  // standard window width
        int windowHeight = 10; // standard window height

        switch (choice) {
            case 0: // Top Wall
                window = new Window(
                    selectedRoom.getX() + (selectedRoom.getWidth() / 2) - (windowWidth / 2),
                    selectedRoom.getY()
                );
                break;
            case 1: // Bottom Wall
                window = new Window(
                    selectedRoom.getX() + (selectedRoom.getWidth() / 2) - (windowWidth / 2),
                    selectedRoom.getY() + selectedRoom.getHeight() - windowHeight
                );
                break;
            case 2: // Left Wall
                window = new Window(
                    selectedRoom.getX(),
                    selectedRoom.getY() + (selectedRoom.getHeight() / 2) - (windowWidth / 2)
                );
                window.setWidth(windowHeight);
                window.setHeight(windowWidth);
                break;
            case 3: // Right Wall
                window = new Window(
                    selectedRoom.getX() + selectedRoom.getWidth() - windowHeight,
                    selectedRoom.getY() + (selectedRoom.getHeight() / 2) - (windowWidth / 2)
                );
                window.setWidth(windowHeight);
                window.setHeight(windowWidth);
                break;
        }

        if (window != null && canAddWindow(selectedRoom, window, rooms)) {
            selectedRoom.addWindow(window);
            canvasPanel.repaint();
        } else {
            JOptionPane.showMessageDialog(this,
                "Cannot add window here. Windows must be on external walls and cannot overlap.",
                "Invalid Window Placement",
                JOptionPane.WARNING_MESSAGE);
        }
    } else {
        JOptionPane.showMessageDialog(this, "Please select a room first.");
    }
}

        private void addFixture() {
            if (selectedRoom != null) {
                String selectedFixtureType = (String) fixtureTypeComboBox.getSelectedItem();
                
                // Calculate initial position (centered in the room)
                Furniture newFixture;
                int fixtureWidth = 50; // default width
                int fixtureHeight = 35; // default height
                
                // Set dimensions based on fixture type
                switch (selectedFixtureType.toUpperCase()) {
                    case "COMMODE":
                        fixtureWidth = 50;
                        fixtureHeight = 35;
                        break;
                    case "WASHBASIN":
                        fixtureWidth = 40;
                        fixtureHeight = 30;
                        break;
                    case "SHOWER":
                        fixtureWidth = 60;
                        fixtureHeight = 60;
                        break;
                    case "KITCHEN SINK":
                        fixtureWidth = 40;
                        fixtureHeight = 30;
                        break;
                    case "STOVE":
                        fixtureWidth = 60;
                        fixtureHeight = 30;
                        break;
                }
                
                // Calculate center position in the room
                int centerX = selectedRoom.getX() + (selectedRoom.getWidth() - fixtureWidth) / 2;
                int centerY = selectedRoom.getY() + (selectedRoom.getHeight() - fixtureHeight) / 2;
                
                // Create the fixture with the calculated dimensions
                newFixture = new Furniture(centerX, centerY, selectedFixtureType);
                // newFixture.width = fixtureWidth;
                // newFixture.height = fixtureHeight;
                
                // Check if fixture fits in the room
                if (fixtureWidth > selectedRoom.getWidth() || 
                    fixtureHeight > selectedRoom.getHeight()) {
                    JOptionPane.showMessageDialog(this, 
                        "This fixture is too large for the selected room.",
                        "Size Error",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Check if the fixture is within room bounds and not overlapping
                if (isFurnitureWithinRoom(newFixture, selectedRoom) && 
                    !selectedRoom.isOverlapping(newFixture)) {
                    selectedRoom.addFurniture(newFixture);
                    selectedFurniture = newFixture; // Select the newly added fixture
                    canvasPanel.repaint();
                } else {
                    // Try to find a valid position
                    if (findValidPosition(newFixture, selectedRoom)) {
                        selectedRoom.addFurniture(newFixture);
                        selectedFurniture = newFixture;
                        canvasPanel.repaint();
                    } else {
                        JOptionPane.showMessageDialog(this,
                            "Cannot place fixture in room. No valid position found.",
                            "Placement Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a room first.");
            }
        }

        private boolean findValidPosition(Furniture furniture, Room room) {
            int margin = 10; // Minimum distance from walls
            int stepSize = 20; // Distance to move when trying new positions
            
            for (int y = room.getY() + margin; y <= room.getY() + room.getHeight() - furniture.getHeight() - margin; y += stepSize) {
                for (int x = room.getX() + margin; x <= room.getX() + room.getWidth() - furniture.getWidth() - margin; x += stepSize) {
                    furniture.setX(x);
                    furniture.setY(y);
                    
                    if (!room.isOverlapping(furniture)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isFurnitureWithinRoom(Furniture furniture, Room room) {
            // Get the bounds considering rotation
            AffineTransform transform = new AffineTransform();
            transform.rotate(Math.toRadians(furniture.getRotation()),
                            furniture.getX() + furniture.getWidth()/2,
                            furniture.getY() + furniture.getHeight()/2);
            
            Rectangle bounds = new Rectangle(furniture.getX(), furniture.getY(),
                                           furniture.getWidth(), furniture.getHeight());
            Shape rotatedBounds = transform.createTransformedShape(bounds);
            Rectangle roomBounds = room.getBounds();
            
            // Add margin from walls
            int margin = 5;
            roomBounds.grow(-margin, -margin);
            
            return roomBounds.contains(rotatedBounds.getBounds());
        }
        private void addFurniture() {
    if (selectedRoom != null) {
        String selectedFurnitureType = (String) furnitureTypeComboBox.getSelectedItem();
        int x = selectedRoom.getX() + 10;
        int y = selectedRoom.getY() + 10;
        
        boolean foundPosition = false;
        Furniture newFurniture = null;
        
        while (!foundPosition) {
            newFurniture = new Furniture(x, y, selectedFurnitureType);
            
            if (!selectedRoom.isOverlapping(newFurniture)) {
                selectedRoom.addFurniture(newFurniture);
                selectedFurniture = newFurniture;  // Select the newly added furniture
                canvasPanel.repaint();
                foundPosition = true;
            } else {
                x += 50;
                if (x + 40 > selectedRoom.getX() + selectedRoom.getWidth()) {
                    x = selectedRoom.getX() + 10;
                    y += 50;
                }
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
    if (selectedFurniture == null || selectedRoom == null) {
        JOptionPane.showMessageDialog(this,
            "Please select a piece of furniture to rotate.",
            "No Selection",
            JOptionPane.INFORMATION_MESSAGE);
        return;
    }

    // Store original rotation
    double oldRotation = selectedFurniture.getRotation();
    
    // Perform rotation
    selectedFurniture.rotate();
    
    // Check if new position causes overlap
    if (selectedRoom.isOverlapping(selectedFurniture)) {
        // If overlap occurs, revert rotation
        selectedFurniture.rotateBack();
        JOptionPane.showMessageDialog(this,
            "Cannot rotate furniture here, it would overlap with existing elements.",
            "Rotation Error",
            JOptionPane.WARNING_MESSAGE);
    } else {
        // If no overlap, apply rotation
        canvasPanel.repaint();
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
                // Draw room interior
                g2d.setColor(room.getColor());
                g2d.fillRect(room.getX(), room.getY(), room.getWidth(), room.getHeight());
                
                // Draw thick walls
                g2d.setColor(Color.DARK_GRAY);
                int wallThickness = 8; // Thickness of the walls
                
                // Draw outer walls
                g2d.fillRect(room.getX() - wallThickness/2, room.getY() - wallThickness/2, 
                            room.getWidth() + wallThickness, wallThickness); // Top wall
                g2d.fillRect(room.getX() - wallThickness/2, room.getY() + room.getHeight() - wallThickness/2, 
                            room.getWidth() + wallThickness, wallThickness); // Bottom wall
                g2d.fillRect(room.getX() - wallThickness/2, room.getY() - wallThickness/2, 
                            wallThickness, room.getHeight() + wallThickness); // Left wall
                g2d.fillRect(room.getX() + room.getWidth() - wallThickness/2, room.getY() - wallThickness/2, 
                            wallThickness, room.getHeight() + wallThickness); // Right wall
                
                // Draw doors
                for (Door door : room.getDoors()) {
                    drawDoor(g2d, door, room);
                }
                g2d.setColor(Color.BLACK);
        Stroke oldStroke = g2d.getStroke();
        float[] dash = {5.0f, 5.0f};
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 
            10.0f, dash, 0.0f));

        for (Window window : room.getWindows()) {
            // Draw the window frame
            g2d.drawRect(window.getX(), window.getY(), window.getWidth(), window.getHeight());
            
            // Draw the window line
            if (window.getWidth() > window.getHeight()) {
                // Horizontal window
                int midY = window.getY() + window.getHeight() / 2;
                g2d.drawLine(window.getX(), midY, 
                            window.getX() + window.getWidth(), midY);
            } else {
                // Vertical window
                int midX = window.getX() + window.getWidth() / 2;
                g2d.drawLine(midX, window.getY(), 
                            midX, window.getY() + window.getHeight());
            }
        }
        
        g2d.setStroke(oldStroke);
                
                // Reset stroke
                g2d.setStroke(new BasicStroke(1));
                
                // Draw furniture
                for (Furniture furniture : room.getFurnitures()) {
                    drawFurniture(g2d, furniture);
                }
            }
        }

        // Replace the existing drawDoor method in FloorPlanner class
private void drawDoor(Graphics2D g2d, Door door, Room room) {
    // Fill door area with room color to create gap effect
    g2d.setColor(room.getColor());
    g2d.fillRect(door.getX(), door.getY(), door.getWidth(), door.getHeight());
    
    // Draw door swing arc and door representation
    g2d.setColor(Color.BLACK);
    g2d.setStroke(new BasicStroke(1));
    
    boolean isHorizontal = door.getWidth() > door.getHeight();
    if (isHorizontal) {
        // Draw door swing arc
        g2d.drawArc(door.getX(), door.getY() - 20, 
                    door.getWidth(), 40, 0, 90);
        
        // Draw door line
        g2d.drawLine(door.getX(), door.getY(), 
                    door.getX(), door.getY() + door.getHeight());
    } else {
        // Draw door swing arc
        g2d.drawArc(door.getX() - 20, door.getY(), 
                    40, door.getHeight(), 0, 90);
        
        // Draw door line
        g2d.drawLine(door.getX(), door.getY(), 
                    door.getX() + door.getWidth(), door.getY());
    }
}
        private void drawWindow(Graphics2D g2d, Window window, Room room) {
            // Draw window frame
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRect(window.getX(), window.getY(), window.getWidth(), window.getHeight());
            
            // Draw window lines
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRect(window.getX(), window.getY(), window.getWidth(), window.getHeight());
            
            // Draw middle line
            int middleX = window.getX() + window.getWidth() / 2;
            g2d.drawLine(middleX, window.getY(), middleX, window.getY() + window.getHeight());
        }
private void drawFurniture(Graphics2D g2d, Furniture furniture) {
    // Save the current transform
    AffineTransform oldTransform = g2d.getTransform();
    
    // Calculate the center point of the furniture
    int centerX = furniture.getX() + furniture.getWidth() / 2;
    int centerY = furniture.getY() + furniture.getHeight() / 2;
    
    // Create a new transform for rotation
    AffineTransform transform = new AffineTransform();
    transform.rotate(Math.toRadians(furniture.getRotation()), centerX, centerY);
    g2d.transform(transform);

    // Draw furniture with image if available, otherwise draw placeholder
    ImageIcon icon = getIconForFurniture(furniture.getType());
    if (icon != null) {
        g2d.drawImage(icon.getImage(), 
                      furniture.getX(), furniture.getY(), 
                      furniture.getWidth(), furniture.getHeight(), null);
    } else {
        g2d.setColor(Color.GRAY);
        g2d.fillRect(furniture.getX(), furniture.getY(), 
                     furniture.getWidth(), furniture.getHeight());
    }
    
    // Highlight selected furniture
    if (furniture == selectedFurniture) {
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRect(furniture.getX(), furniture.getY(), 
                     furniture.getWidth(), furniture.getHeight());
    }

    // Restore the original transform
    g2d.setTransform(oldTransform);
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
        fileChooser.setFileFilter(new FileNameExtensionFilter("Floor Plan Files (*.fplan)", "fplan"));
        
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            // Add extension if not present
            if (!fileToSave.getAbsolutePath().toLowerCase().endsWith(".fplan")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".fplan");
            }
    
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileToSave))) {
                // Create a data structure to hold all necessary information
                HashMap<String, Object> planData = new HashMap<>();
                planData.put("rooms", rooms);
                
                // Save the plan data
                oos.writeObject(planData);
                
                updateStatus("Plan saved successfully to " + fileToSave.getName());
                JOptionPane.showMessageDialog(this, 
                    "Floor plan saved successfully.", 
                    "Save Successful", 
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (IOException e) {
                String errorMessage = "Error saving floor plan: " + e.getMessage();
                updateStatus(errorMessage);
                JOptionPane.showMessageDialog(this, 
                    errorMessage, 
                    "Save Error", 
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

@SuppressWarnings("unchecked")
    private void loadPlan() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Floor Plan");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Floor Plan Files (*.fplan)", "fplan"));
        
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToLoad = fileChooser.getSelectedFile();
            
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileToLoad))) {
                // Clear current selection
                selectedRoom = null;
                selectedFurniture = null;
                
                // Clear existing rooms
                rooms.clear();
                
                // Load the plan data
                HashMap<String, Object> planData = (HashMap<String, Object>) ois.readObject();
                
                // Update rooms
                rooms = (ArrayList<Room>) planData.get("rooms");
                
                // Force immediate refresh of the UI
                SwingUtilities.invokeLater(() -> {
                    // Repaint the canvas panel
                    canvasPanel.revalidate();
                    canvasPanel.repaint();
                    
                    // Repaint the entire frame
                    revalidate();
                    repaint();
                    
                    updateStatus("Plan loaded successfully from " + fileToLoad.getName());
                    
                    JOptionPane.showMessageDialog(this, 
                        "Floor plan loaded successfully.", 
                        "Load Successful", 
                        JOptionPane.INFORMATION_MESSAGE);
                });
                    
            } catch (Exception e) {
                String errorMessage = "Error loading floor plan: " + e.getMessage();
                updateStatus(errorMessage);
                JOptionPane.showMessageDialog(this, 
                    errorMessage, 
                    "Load Error", 
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    private void updateStatus(String message) {
            for (Component c : topToolBar.getComponents()) {
                if (c instanceof JLabel && c != topToolBar.getComponent(2)) { // Skip the title label
                    ((JLabel) c).setText(message);
                    break;
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
        private Map<Door, Point> originalDoorPositions;
        private Map<Window, Point> originalWindowPositions;
        private Map<Furniture, Point> originalFurniturePositions;
        int wallThickness = 8;
    
        public Room(int x, int y, int width, int height, RoomType type) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.type = type;
            this.doors = new ArrayList<>();
            this.windows = new ArrayList<>();
            this.furnitures = new ArrayList<>();
            this.originalDoorPositions = new HashMap<>();
            this.originalWindowPositions = new HashMap<>();
            this.originalFurniturePositions = new HashMap<>();
        }
    
        // Fixed getType() method
        
        // Other getters and setters
        public RoomType getType() {
            return type;
        }
    
        public int getX() {
            return x;
        }
    
        public void setX(int x) {
            this.x = x;
        }
    
        public int getY() {
            return y;
        }
    
        public void setY(int y) {
            this.y = y;
        }
    
        public int getWidth() {
            return width;
        }
    
        public void setWidth(int width) {
            this.width = width;
        }
    
        public int getHeight() {
            return height;
        }
    
        public void setHeight(int height) {
            this.height = height;
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
    
        public void addDoor(Door door) {
            doors.add(door);
        }
    
        public void addWindow(Window window) {
            windows.add(window);
        }
    
        public void addFurniture(Furniture furniture) {
            furnitures.add(furniture);
        }
    
        public Rectangle getBounds() {
            // Include walls in the room bounds
            return new Rectangle(
                x - wallThickness/2, 
                y - wallThickness/2, 
                width + wallThickness, 
                height + wallThickness
            );
        }

        public void moveElements(int deltaX, int deltaY) {
            // Move doors
            for (Door door : doors) {
                door.setX(door.getX() + deltaX);
                door.setY(door.getY() + deltaY);
            }
            
            // Move windows
            for (Window window : windows) {
                window.setX(window.getX() + deltaX);
                window.setY(window.getY() + deltaY);
            }
            
            // Move furniture
            for (Furniture furniture : furnitures) {
                furniture.setX(furniture.getX() + deltaX);
                furniture.setY(furniture.getY() + deltaY);
            }
        }
        // Add this method to the Room class:
public boolean isFurnitureOverlapping(Furniture furniture) {
    // Check if furniture overlaps with any other furniture in the room
    for (Furniture existing : furnitures) {
        if (existing != furniture && furniture.intersects(existing)) {
            return true;
        }
    }
    
    // Check if furniture overlaps with doors or windows
    Rectangle furnitureBounds = furniture.getBounds();
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
    
    return false;
}

        public void draw(Graphics2D g2d) {
            // Draw room walls
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2)); // Thicker walls
        
            // Draw walls with gaps for doors
            drawWallsWithDoors(g2d);
            
            // Draw windows as dashed lines
            g2d.setColor(Color.BLUE);
            float[] dash = {5.0f}; // Dash pattern
            g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 
                                         10.0f, dash, 0.0f));
            for (Window window : windows) {
                g2d.drawLine(window.getX(), window.getY(), 
                            window.getX() + window.getWidth(), window.getY());
            }
            
            // Reset stroke for other drawings
            g2d.setStroke(new BasicStroke());
        }
        private void drawWallsWithDoors(Graphics2D g2d) {
            // Top wall
            drawWallSegmentWithDoors(g2d, x, y, x + width, y, true);
            
            // Right wall
            drawWallSegmentWithDoors(g2d, x + width, y, x + width, y + height, false);
            
            // Bottom wall
            drawWallSegmentWithDoors(g2d, x, y + height, x + width, y + height, true);
            
            // Left wall
            drawWallSegmentWithDoors(g2d, x, y, x, y + height, false);
        }

        private void drawWallSegmentWithDoors(Graphics2D g2d, int x1, int y1, int x2, int y2, boolean horizontal) {
            for (Door door : doors) {
                Rectangle doorBounds = door.getBounds();
                if (horizontal) {
                    if (doorBounds.y == y1) {
                        // Draw wall segments around the door
                        g2d.drawLine(x1, y1, doorBounds.x, y1);
                        g2d.drawLine(doorBounds.x + doorBounds.width, y1, x2, y1);
                        return;
                    }
                } else {
                    if (doorBounds.x == x1) {
                        // Draw wall segments around the door
                        g2d.drawLine(x1, y1, x1, doorBounds.y);
                        g2d.drawLine(x1, doorBounds.y + doorBounds.height, x1, y2);
                        return;
                    }
                }
            }
            // If no door on this wall, draw complete wall
            g2d.drawLine(x1, y1, x2, y2);
        }


        public void drawWalls(Graphics g) {
        g.setColor(Color.DARK_GRAY);
        // Outer wall
        g.fillRect(x, y, width + wallThickness, wallThickness); // Top
        g.fillRect(x, y + height, width + wallThickness, wallThickness); // Bottom
        g.fillRect(x, y, wallThickness, height + wallThickness); // Left
        g.fillRect(x + width, y, wallThickness, height + wallThickness); // Right
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
    
        // Restore original positions of all elements
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
    // Replace the existing canAddDoor method in Room class
public boolean canAddDoor(Door door, ArrayList<Room> rooms) {
    // Check if door is on wall
    boolean onWall = false;
    int tolerance = wallThickness/2;
    
    // Check top/bottom walls
    if (Math.abs(door.getY() - y) <= tolerance || 
        Math.abs(door.getY() - (y + height)) <= tolerance) {
        onWall = (door.getX() >= x - tolerance && 
                 door.getX() + door.getWidth() <= x + width + tolerance);
    }
    // Check left/right walls
    else if (Math.abs(door.getX() - x) <= tolerance || 
             Math.abs(door.getX() - (x + width)) <= tolerance) {
        onWall = (door.getY() >= y - tolerance && 
                 door.getY() + door.getHeight() <= y + height + tolerance);
    }
    
    // Check if door overlaps with existing doors or windows
    if (onWall) {
        Rectangle doorBounds = door.getBounds();
        doorBounds.grow(tolerance, tolerance);
        
        for (Door existingDoor : doors) {
            if (doorBounds.intersects(existingDoor.getBounds())) {
                return false;
            }
        }
        
        for (Window window : windows) {
            if (doorBounds.intersects(window.getBounds())) {
                return false;
            }
        }
    }
    
    return onWall;
}
    public boolean canAddWindow(Window window) {
    // Check if window is on any wall
    boolean onWall = false;
    Rectangle roomBounds = getBounds();
    Rectangle windowBounds = window.getBounds();
    
    // Add small tolerance for wall detection
    int tolerance = 2;
    
    // Check if window is on top or bottom wall
    if (Math.abs(window.getY() - y) <= tolerance || 
        Math.abs(window.getY() - (y + height - window.getHeight())) <= tolerance) {
        onWall = (window.getX() >= x && 
                 window.getX() + window.getWidth() <= x + width);
    }
    // Check if window is on left or right wall
    else if (Math.abs(window.getX() - x) <= tolerance || 
             Math.abs(window.getX() - (x + width - window.getWidth())) <= tolerance) {
        onWall = (window.getY() >= y && 
                 window.getY() + window.getHeight() <= y + height);
    }
    
    if (!onWall) {
        return false;
    }
    
    // Check for overlap with other windows and doors
    for (Window existingWindow : windows) {
        if (windowBounds.intersects(existingWindow.getBounds())) {
            return false;
        }
    }
    
    for (Door door : doors) {
        if (windowBounds.intersects(door.getBounds())) {
            return false;
        }
    }
    
    return true;
}


    public boolean isOverlapping(Door door) {
        Rectangle doorBounds = door.getBounds();
        Rectangle roomBoundsWithWalls = getBounds();
        
        // Check if door is within the walls
        if (!roomBoundsWithWalls.contains(doorBounds)) {
            return true;
        }
        
        // Check overlap with other elements
        for (Door existingDoor : doors) {
            if (doorBounds.intersects(existingDoor.getBounds())) {
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
    private static final int DEFAULT_WIDTH = 40;
    private static final int DEFAULT_HEIGHT = 10;

    public Window(int x, int y) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // Add all getter and setter methods
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

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}

    class Furniture implements Serializable {
    private static final long serialVersionUID = 1L;
    private int x, y, width, height;
    private String type;
    private double rotation = 0;  // Changed to double for more precise rotation
    private transient Image image;
    private String imagePath;

    public Furniture(int x, int y, String type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.rotation = 0;

        switch (type.toUpperCase()) {
            case "BED":
                this.width = 50;
                this.height = 50;
                break;
            case "CHAIR":
                this.width = 40;
                this.height = 40;
                break;
            case "TABLE":
                this.width = 50;
                this.height = 50;
                break;
            case "SOFA":
                this.width = 50;
                this.height = 50;
                break;
            case "DINING SET":
                this.width = 50;
                this.height = 50;
                break;
            case "COMMODE":
                this.width = 50;
                this.height = 35;
                break;
            case "WASHBASIN":
                this.width = 40;
                this.height = 30;
                break;
            case "SHOWER":
                this.width = 60;
                this.height = 60;
                break;
            case "KITCHEN SINK":
                this.width = 50;
                this.height = 30;
                break;
            case "STOVE":
                this.width = 60;
                this.height = 30;
                break;
            default:
                this.width = 50;
                this.height = 50;
        }
    }

    // Existing getters and setters remain the same
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getType() { return type; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public double getRotation() { return rotation; }

    public void rotate() {
        rotation = (rotation + 90) % 360;
    }

    public void rotateBack() {
        rotation = (rotation - 90 + 360) % 360;
    }

    public Rectangle getBounds() {
        // For collision detection, we use the larger of width/height for rotated furniture
        int size = Math.max(width, height);
        return new Rectangle(x, y, size, size);
    }

    public boolean intersects(Furniture other) {
        return this.getBounds().intersects(other.getBounds());
    }

    public boolean isWithinRoom(Room room) {
        Rectangle roomBounds = room.getBounds();
        Rectangle furnitureBounds = this.getBounds();
        
        // Add padding for walls
        roomBounds.x += room.wallThickness;
        roomBounds.y += room.wallThickness;
        roomBounds.width -= 2 * room.wallThickness;
        roomBounds.height -= 2 * room.wallThickness;
        
        return roomBounds.contains(furnitureBounds);
    }
}