/*
  Java 1.1 AWT Applet - Maze Fog Game
  Written by: Keith Fenske, http://www.psc-consulting.ca/fenske/
  Wednesday, 28 January 2004
  Java class name: MazeFog2
  Copyright (c) 2004 by Keith Fenske.  Released under GNU Public License.

  This is a graphical Java 1.1 AWT (GUI) applet to play a maze game.  The
  computer creates a random maze.  The exit is marked by blue and white boxes.
  Your position is marked by a blue circle.  Use the arrow keys or the mouse to
  move towards the exit.  You may have to reposition your mouse if you bump
  into walls!  You may run this program as a stand-alone application, or as an
  applet on the following web page:

      Maze Fog Game - by: Keith Fenske
      http://www.psc-consulting.ca/fenske/mazfog2a.htm

  There are no monsters or obstacles in the maze.  Your view is limited by a
  "fog" that shows only nearby positions and positions that you have already
  visited.  If you can't solve the maze, then click the "Show Me" button to see
  the path to the exit.

  GNU General Public License (GPL)
  --------------------------------
  MazeFog2 is free software: you can redistribute it and/or modify it under the
  terms of the GNU General Public License as published by the Free Software
  Foundation, either version 3 of the License or (at your option) any later
  version.  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
  more details.

  You should have received a copy of the GNU General Public License along with
  this program.  If not, see the http://www.gnu.org/licenses/ web page.

  -----------------------------------------------------------------------------

  Programming Notes:

  Since every line is shared by two squares (except for lines on the outside
  edges of the board), assign all horizontal lines to the top edge of a square
  and all vertical lines to the left edge of a square.  This makes the program
  data more consistent.

  There is one more column of vertical lines than there are columns of board
  squares, so add an invisible column of board squares on the right side to
  hold the extra "left" lines.  Similarly, add an invisible row of board
  squares on the bottom to hold the extra "top" lines.

  -----------------------------------------------------------------------------

  Java Applet Notes:

  The recommended way of writing applets is to use Java Swing, according to Sun
  Microsystems, the creators and sponsors of Java.  Unfortunately, most web
  browsers don't support Swing unless you download a recent copy of the Java
  run-time environment from Sun.  This leaves a Java programmer with two
  choices:

  (1) Write applets using only old features found in the AWT interface.  The
      advantage, if you can see it this way, is that the programmer gets a
      detailed opportunity to interact with the graphical interface.  (Joke.)

  (2) Force users to visit http://java.sun.com/downloads/ to download and
      install a newer version of Java.  However, forcing anyone to download
      something before they can visit your web page is a poor idea.

  A worse idea is new browsers that don't have any Java support at all, unless
  the user first downloads Sun Java.  Microsoft stopped distributing their
  version of Java in 2003 starting with Windows XP SP1a (February), then
  Windows 2000 SP4 (June).  Until Microsoft and Sun resolve their various
  lawsuits -- or until Microsoft agrees to distribute an unaltered version of
  Sun Java -- there will be an increasing number of internet users that have
  *no* version of Java installed on their machines!

  The design considerations for this applet are as follows:

  (1) The applet should run on older browsers as-is, without needing any
      additional downloads and/or features.  The minimum target is JDK1.1 which
      is Microsoft Internet Explorer 5.0 (Windows 98) and Netscape 4.7/4.8 (JDK
      1.1.5 from 1997).

  (2) Unlike the previous Life3 and TicTacToe4 applets, this applet uses more
      than one class.  A second class, a subclass of Canvas, is used to better
      draw and accept mouse input on the game board.  To run this applet on a
      web page, MazeFog2 should be loaded from a JAR (Java archive) file.

  (3) The default background in the Sun Java applet viewer is white, but most
      web browsers use light grey.  To get the background color that you want,
      you must setBackground() on components or fillRect() with the color of
      your choice.

  (4) A small main() method is included with a WindowAdapter subclass, so that
      this program can be run as an application.  The default window size and
      position won't please everyone.

  (5) We play a sample sound when the user reaches the exit.  Most newer web
      browsers are smart enough to load this sound file from the applet's JAR
      file, but not Netscape 4.7/4.8 (JDK1.1) which expects to find the sound
      as a separate file on the web server.  That means putting the sound file
      in two places: once inside the JAR (for faster loading on most browsers)
      and once by itself on the web site.  Note also that sound clips are
      loaded differently in applets and applications.
*/

import java.applet.*;             // older Java applet support
import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support

public class MazeFog2
       extends Applet
       implements ActionListener, KeyListener
{
  /* constants */

  static final int canvasBorder = 10; // empty pixels around game board
  static final String noMessage = " "; // message text when nothing to say
  static final int[] sizeList = {10, 13, 16, 20, 25, 32, 40, 50, 63, 80};
                                  // defined sizes in pixels of board squares
  static final String winSoundString = "MAZFOG2E.AU";
                                  // Play this sound clip when the user wins:
                                  // a renamed "danger.au" from the Java SDK,
                                  // which sounds like cow bells.  Must match
                                  // the exact file name (lower- and uppercase)
                                  // in some applet viewers.

  static final Color BACKGROUND = new Color(255, 204, 204); // light pink
  static final Color ColorEXIT1 = new Color(204, 255, 255); // cyan
  static final Color ColorEXIT2 = new Color(102, 51, 204); // off blue
  static final Color ColorLINE = new Color(153, 102, 102); // darker pink
  static final Color ColorUSER = new Color(102, 102, 255); // light blue

  static final int DelayDRAW = 5; // milliseconds while drawing board
  static final int DelayFLASH = 100; // milliseconds while flashing display

  static final int GameACTIVE = 101; // waiting for user to move
  static final int GameFINISH = 102; // game is finished (no moves allowed)

  static final int LineEMPTY = 201; // no horizontal or vertical line
  static final int LineHIDDEN = 202; // line exists, but is currently hidden
  static final int LineVISIBLE = 203; // line is visible to the user

  /* class variables */

  static AudioClip winSound = null; // non-null when sound clip has been loaded

  /* instance variables, including shared GUI components */

  Button biggerButton;            // "Bigger" button
  int boardBorderSize;            // size in pixels of border between board
                                  // ... lines and board symbols
  Canvas boardCanvas;             // where we draw the game board
  int[][] boardDistance;          // distance from exit (in squares), or -1 if
                                  // ... not on a path
  int boardGridSize;              // size in pixels of each board square,
                                  // ... including one set of lines
  int[][] boardLeft;              // left (vertical) lines
  int boardLineWidth;             // width in pixels of board lines
  int boardSymbolSize;            // size in pixels of board symbols
  int[][] boardTop;               // top (horizontal) lines
  int exitCol;                    // column number of exit (goal) square
  int exitRow;                    // row number of exit (goal) square
  int gameState;                  // state variable for current game
  Label messageText;              // information or status message for user
  Button newgameButton;           // "New Game" button
  int numCols;                    // number of columns in current game board
  int numRows;                    // number of rows in current game board
  int sizeIndex;                  // index of current entry in <sizeList>
  Button showmeButton;            // "Show Me" button
  Button smallerButton;           // "Smaller" button
  int startCol;                   // user's starting column number
  int startDistance;              // user's starting distance from the exit
  int startRow;                   // user's starting row number
  int userCol;                    // current column number for user's position
  int userColOffset;              // offset from <userCol> in pixels
  int userRow;                    // current row number for user's position
  int userRowOffset;              // offset from <userRow> in pixels


/*
  init() method

  Initialize this applet (equivalent to the main() method in an application).
  Please note the following about writing applets:

  (1) An Applet is an AWT Component just like a Button, Frame, or Panel.  It
      has a width, a height, and you can draw on it (given a proper graphical
      context, as in the paint() method).

  (2) Applets shouldn't attempt to exit, such as by calling the System.exit()
      method, because this isn't allowed on a web page.
*/
  public void init()
  {
    /* Intialize our own data before creating the GUI interface.  Some of these
    values are necessary; others are a precaution in case the GUI gets ahead of
    us before we build a proper maze.  The initial value of all arrays is
    assumed to be null. */

    exitCol = exitRow = 0;        // just to be safe, set some initial value
    gameState = GameFINISH;       // no moves allowed until we are ready
    numCols = numRows = 1;
    startCol = startDistance = startRow = 0;
    userCol = userColOffset = userRow = userRowOffset = 0;

    sizeIndex = 5;                // initial size is near middle of list
    setBoardSizes();              // set sizes of board elements

    /* Load the winning sound now if an application wrapper hasn't already done
    so.  getCodeBase() will throw a NullPointerException if we are not running
    as an applet. */

    if (winSound == null)         // if application hasn't already loaded sound
      try { winSound = getAudioClip(getCodeBase(), winSoundString); }
        catch (NullPointerException except) { winSound = null; }

    /* Create the GUI interface as a series of little panels inside bigger
    panels.  The intermediate panel names (panel1, panel2, etc) are of no
    importance and hence are only numbered. */

    /* Make a horizontal panel to hold four equally-spaced buttons.  We put
    this first panel inside a second FlowLayout panel to prevent the buttons
    from stretching horizontally as the window size gets bigger. */

    Panel panel1 = new Panel(new GridLayout(1, 4, 25, 0));

    biggerButton = new Button("Bigger (B)");
    biggerButton.addActionListener((ActionListener) this);
    panel1.add(biggerButton);

    smallerButton = new Button("Smaller (S)");
    smallerButton.addActionListener((ActionListener) this);
    panel1.add(smallerButton);

    showmeButton = new Button("Show Me (M)");
    showmeButton.addActionListener((ActionListener) this);
    panel1.add(showmeButton);

    newgameButton = new Button("New Game (N)");
    newgameButton.addActionListener((ActionListener) this);
    panel1.add(newgameButton);

    Panel panel2 = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 5));
    panel2.setBackground(BACKGROUND); // for Netscape 4.7/4.8 (JDK1.1)
    panel2.add(panel1);

    /* Put a message field under the buttons. */

    Panel panel3 = new Panel(new GridLayout(2, 1, 0, 0));
    panel3.add(panel2);
    messageText = new Label("Maze Fog (Java applet).  Copyright (c) 2004 by Keith Fenske.  GNU Public License.", Label.CENTER);
    messageText.setFont(new Font("Default", Font.PLAIN, 14));
    messageText.setBackground(BACKGROUND);
    panel3.add(messageText);

    /* Put the buttons and message field on top of a canvas for the game board,
    giving the game board the remaining window space.  We set the applet to
    have a BorderLayout and put <boardCanvas> in the center, which allows the
    canvas to expand and contract with the applet's window size.  Note that the
    MazeFog2Board class assumes that a MazeFog2 object is the parent container
    of <boardCanvas>. */

    boardCanvas = new MazeFog2Board();
    boardCanvas.addKeyListener((KeyListener) this); // listen to arrow keys
    boardCanvas.addMouseMotionListener((MouseMotionListener) boardCanvas);

    this.setLayout(new BorderLayout(0, 0));
    this.add(panel3, BorderLayout.NORTH);
    this.add(boardCanvas, BorderLayout.CENTER);
    this.setBackground(BACKGROUND);
    this.validate();              // do the window layout

    /* Create the initial maze.  Must come after GUI is ready, because we use
    the size of <boardCanvas>. */

    makeBoard(false);             // make maze, don't display as we create

    /* Now let the GUI interface run the game.  A repaint() will occur after
    init() returns, which will display the game board with the appropriate
    parts hidden or visible. */

    boardCanvas.requestFocus();   // set focus so we can listen for arrow keys
    gameState = GameACTIVE;       // let the game begin

  } // end of init() method


/*
  main() method

  Applets only need an init() method to start execution.  This main() method is
  a wrapper that allows the same applet code to run as an application.
*/
  public static void main(String[] args)
  {
    MazeFog2 appletPanel;         // the target applet's window
    Frame mainFrame;              // this application's window

    /* Loading sound clips in an application requires a call to newAudioClip(),
    which is JDK1.2.  Load the winning sound now so that the applet won't fail
    when calling getCodeBase().  As long as the URL syntax is good, <winSound>
    will have a non-null value ... even if the file doesn't exist or can't be
    loaded. */

    try { winSound = newAudioClip(new java.net.URL("file:" + winSoundString)); }
      catch (java.net.MalformedURLException except) { winSound = null; }

    /* Create the frame that will hold the applet. */

    mainFrame = new Frame("Maze Fog Game - by: Keith Fenske");
    mainFrame.addWindowListener(new MazeFog2Window());
    mainFrame.setLayout(new BorderLayout(5, 5));
    mainFrame.setLocation(new Point(50, 50)); // top-left corner of app window
    mainFrame.setSize(700, 500);  // initial size of application window
    appletPanel = new MazeFog2(); // create instance of target applet
    mainFrame.add(appletPanel, BorderLayout.CENTER); // give applet full frame
    mainFrame.validate();         // do the application window layout
    mainFrame.setVisible(true);   // show the application window

    /* Initialize the applet after the layout and sizes of the main frame have
    been determined, since the applet computes how many rows and columns will
    fit into the allocated size of <boardCanvas>. */

    appletPanel.init();           // initialize applet with correct sizes
    appletPanel.boardCanvas.repaint(); // force game board to appear (redraw)

  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  actionPerformed() method

  This method is called when the user clicks on a control button at the top of
  the applet window (not on the game board).
*/
  public void actionPerformed(ActionEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == biggerButton)
      doBiggerButton();           // "Bigger" button for bigger board squares
    else if (source == newgameButton)
      doNewgameButton();          // "New Game" button to start a new game
    else if (source == showmeButton)
      doShowmeButton();           // "Show Me" button to see the solution
    else if (source == smallerButton)
      doSmallerButton();          // "Smaller" button for smaller board squares
    else
    {
      System.out.println(
        "error in actionPerformed(): ActionEvent not recognized: " + event);
    }
  } // end of actionPerformed() method


/*
  boardMouseMoved() method

  This method is called by our dummy Canvas class (MazeFog2Board) to process
  mouse movement across the game board, in the context of the MazeFog2 class.
  While there is an active game, we try to copy the mouse's position to the
  user's position on the game board.

  boardMouseMoved() is a more complicated version of keyPressed() because the
  arrow keys only move in whole board positions (rows and columns).  Since both
  input methods are available to the user, we must leave the user's position in
  a form that is valid for keyPressed().  For example, if the mouse moves more
  than halfway to the next board square, then we must set the row and column to
  the new square, with negative pixel offsets back towards the old square.
*/
  public void boardMouseMoved(MouseEvent event)
  {
    int deltaX;                   // mouse horizontal (x) distance
    int deltaY;                   // mouse vertical (y) distance
    Graphics gr;                  // graphics context for <boardCanvas>
    int halfway;                  // halfway distance to next square in pixels
    int hz;                       // temporary number of horizontal pixels
    int mouseNewX;                // new x coordinate of mouse
    int mouseNewY;                // new y coordinate of mouse
    int userNewX;                 // new x coordinate of user's position
    int userNewY;                 // new y coordinate of user's position
    int userOldX;                 // old x coordinate of user's position
    int userOldY;                 // old y coordinate of user's position
    int vt;                       // temporary number of vertical pixels

    if (gameState == GameACTIVE)  // ignore mouse unless there is an active game
    {
      halfway = boardGridSize / 2; // any offset bigger moves to next square
      mouseNewX = event.getX();   // get mouse x coordinate (horizontal)
      mouseNewY = event.getY();   // get mouse y coordinate (vertical)

      /* Allocate a graphics context for <boardCanvas> and remember the exact
      coordinates of the user's current position, so that we can shift the
      display pixels later.  (Erasing and redrawing the user's board symbol
      every time the mouse moves one pixel would cause a lot of flicker.) */

      gr = boardCanvas.getGraphics(); // get graphics context
      userOldX = (userCol * boardGridSize) + boardBorderSize + boardLineWidth
        + canvasBorder + userColOffset;
      userOldY = (userRow * boardGridSize) + boardBorderSize + boardLineWidth
        + canvasBorder + userRowOffset;

      /* In which direction did the mouse move?  Displacements may be negative,
      zero, or positive.  Assume that the mouse is pointing at the center of
      where the user would like to move.  Note that the coordinates (userOldX,
      userOldY) represent the top-left corner of the bounding box for the
      user's board symbol (the circle), not the center of the board symbol. */

      deltaX = mouseNewX - userOldX - (boardSymbolSize / 2); // horizontal
      deltaY = mouseNewY - userOldY - (boardSymbolSize / 2); // vertical

      /* We are given two endpoints of a straight line: the user's current
      position and the desired new position pointed to by the mouse.  Depending
      upon the speed of the mouse and how busy the system is, the mouse
      movement may be very large and complex but the information available to
      us is only linear.  Curved paths are lost if we aren't called often
      enough.  As a result, we only have to be accurate for short distances and
      straight lines.  Iteratively process the larger of the horizontal and
      vertical movements.  We allow the mouse movement to be quite sloppy. */

      while ((deltaX != 0) || (deltaY != 0))
      {
        if (Math.abs(deltaX) > Math.abs(deltaY)) // more horizontal?
        {
          /* The mouse movement is larger in the horizontal (x) directon. */

          if (deltaX < 0)
          {
            /* We are trying to move left. */

            if (userColOffset > 0) // do we have a right offset pending?
            {
              hz = Math.min(Math.abs(deltaX), Math.abs(userColOffset));
                                  // select smaller segment (absolute value)
              deltaX += hz;       // reduce horizontal distance to move
              userColOffset -= hz; // reduce right offset pending
            }
            else if ((boardLeft[userRow][userCol] != LineEMPTY) // wall?
            || (boardDistance[userRow][userCol - 1] <= 0)) // stop at exit
            {
              deltaX = 0;         // can't go left, so cancel left movement
            }
            else
            {
              /* We can go left.  Limit ourself to a segment that is less than
              or equal to the size of a board square.  Remember, this
              processing is iterative: the remaining distance will be done
              later. */

              hz = Math.abs(userColOffset + deltaX); // total desired
              hz = Math.min(hz, boardGridSize); // smaller segment
              deltaX += hz - Math.abs(userColOffset); // reduce remaining
              if (hz < halfway)   // still on same board square?
                userColOffset = - hz; // yes
              else
              {
                /* We have gone past the halfway point, so switch to the next
                board square. */

                userCol --;       // go one column to the left
                userColOffset = boardGridSize - hz; // now a right offset
              }
            }
          }
          else if (deltaX > 0)
          {
            /* We are trying to move right. */

            if (userColOffset < 0) // do we have a left offset pending?
            {
              hz = Math.min(Math.abs(deltaX), Math.abs(userColOffset));
                                  // select smaller segment (absolute value)
              deltaX -= hz;       // reduce horizontal distance to move
              userColOffset += hz; // reduce left offset pending
            }
            else if ((boardLeft[userRow][userCol + 1] != LineEMPTY) // wall?
            || (boardDistance[userRow][userCol + 1] <= 0)) // stop at exit
            {
              deltaX = 0;         // can't go right, so cancel right movement
            }
            else
            {
              /* We can go right. */

              hz = Math.abs(userColOffset + deltaX); // total desired
              hz = Math.min(hz, boardGridSize); // smaller segment
              deltaX -= hz - Math.abs(userColOffset); // reduce remaining
              if (hz < halfway)   // still on same board square?
                userColOffset = hz; // yes
              else
              {
                /* We have gone past the halfway point, so switch to the next
                board square. */

                userCol ++;       // go one column to the right
                userColOffset = - (boardGridSize - hz); // now a left offset
              }
            }
          }
          else
          {
            /* Amazing.  <deltaX> is zero, but somehow it's still greater than
            the absolute value of <deltaY>, which is not zero! */

            System.out.println("error in boardMouseMoved(): <deltaX> is zero but greater than absolute of <deltaY>");
          }
        }
        else
        {
          /* The mouse movement is larger in the vertical (y) direction. */

          if (deltaY < 0)
          {
            /* We are trying to move up. */

            if (userRowOffset > 0) // do we have a down offset pending?
            {
              vt = Math.min(Math.abs(deltaY), Math.abs(userRowOffset));
                                  // select smaller segment (absolute value)
              deltaY += vt;       // reduce vertical distance to move
              userRowOffset -= vt; // reduce down offset pending
            }
            else if ((boardTop[userRow][userCol] != LineEMPTY) // wall?
            || (boardDistance[userRow - 1][userCol] <= 0)) // stop at exit
            {
              deltaY = 0;         // can't go up, so cancel up movement
            }
            else
            {
              /* We can go up. */

              vt = Math.abs(userRowOffset + deltaY); // total desired
              vt = Math.min(vt, boardGridSize); // smaller segment
              deltaY += vt - Math.abs(userRowOffset); // reduce remaining
              if (vt < halfway)   // still on same board square?
                userRowOffset = - vt; // yes
              else
              {
                /* We have gone past the halfway point, so switch to the next
                board square. */

                userRow --;       // go one column up
                userRowOffset = boardGridSize - vt; // now a down offset
              }
            }
          }
          else if (deltaY > 0)
          {
            /* We are trying to move down. */

            if (userRowOffset < 0) // do we have an up offset pending?
            {
              vt = Math.min(Math.abs(deltaY), Math.abs(userRowOffset));
                                  // select smaller segment (absolute value)
              deltaY -= vt;       // reduce vertical distance to move
              userRowOffset += vt; // reduce up offset pending
            }
            else if ((boardTop[userRow + 1][userCol] != LineEMPTY) // wall?
            || (boardDistance[userRow + 1][userCol] <= 0)) // stop at exit
            {
              deltaY = 0;         // can't go down, so cancel down movement
            }
            else
            {
              /* We can go down. */

              vt = Math.abs(userRowOffset + deltaY); // total desired
              vt = Math.min(vt, boardGridSize); // smaller segment
              deltaY -= vt - Math.abs(userRowOffset); // reduce remaining
              if (vt < halfway)   // still on same board square?
                userRowOffset = vt; // yes
              else
              {
                /* We have gone past the halfway point, so switch to the next
                board square. */

                userRow ++;       // go one column down
                userRowOffset = - (boardGridSize - vt); // now an up offset
              }
            }
          }
          else
          {
            /* We can only get here if the while loop fails and both <deltaX>
            and <deltaY> are zero. */

            System.out.println("error in boardMouseMoved(): both <deltaX> and <deltaY> are zero");
          }
        }
      }

      /* Cancel any pixel offsets that would bump us into a wall.  The most
      obvious cases are being beside a wall (line).  We must also allow for the
      corners at intersections. */

      if (userColOffset < 0)
      {
        /* There is a pixel offset trying to move left.  There may also be a
        vertical offset (not yet tested). */

        if (boardLeft[userRow][userCol] != LineEMPTY)
        {
          /* There is a wall to the left of us.  No left movement allowed. */

          userColOffset = 0;      // cancel horizontal pixel offset
        }
        else if (userRowOffset < 0)
        {
          /* Left is empty.  There is a pixel offset trying to move up. */

          if (boardTop[userRow][userCol] != LineEMPTY) // can we go up?
            userRowOffset = 0;    // cancel vertical pixel offset
          else if ((Math.abs(userColOffset) > boardBorderSize)
          || (Math.abs(userRowOffset) > boardBorderSize))
          {
            /* We round the corners a bit, but once the pixel offsets get too
            big, we have to pick one direction over the other.  How much we can
            round the corners is limited by <boardBorderSize> because even
            though the user's position appears as a circle, this circle is
            shifted around the display inside a square box, and the corners of
            that box might clip lines in the maze. */

            if (Math.abs(userColOffset) > Math.abs(userRowOffset))
              userRowOffset = 0;  // keep larger horizontal offset
            else
              userColOffset = 0;  // keep larger vertical offset
          }
        }
        else if (userRowOffset > 0)
        {
          /* Left is empty.  There is a pixel offset trying to move down. */

          if (boardTop[userRow + 1][userCol] != LineEMPTY) // can we go down?
            userRowOffset = 0;    // cancel vertical pixel offset
          else if ((Math.abs(userColOffset) > boardBorderSize)
          || (Math.abs(userRowOffset) > boardBorderSize))
          {
            /* Rounding a corner, select one direction over the other. */

            if (Math.abs(userColOffset) > Math.abs(userRowOffset))
              userRowOffset = 0;  // keep larger horizontal offset
            else
              userColOffset = 0;  // keep larger vertical offset
          }
        }
      }
      else if (userColOffset > 0)
      {
        /* There is a pixel offset trying to move right.  There may also be a
        vertical offset (not yet tested). */

        if (boardLeft[userRow][userCol + 1] != LineEMPTY)
        {
          /* There is a wall to the right of us.  No right movement allowed. */

          userColOffset = 0;      // cancel horizontal pixel offset
        }
        else if (userRowOffset < 0)
        {
          /* Right is empty.  There is a pixel offset trying to move up. */

          if (boardTop[userRow][userCol] != LineEMPTY) // can we go up?
            userRowOffset = 0;    // cancel vertical pixel offset
          else if ((Math.abs(userColOffset) > boardBorderSize)
          || (Math.abs(userRowOffset) > boardBorderSize))
          {
            /* Rounding a corner, select one direction over the other. */

            if (Math.abs(userColOffset) > Math.abs(userRowOffset))
              userRowOffset = 0;  // keep larger horizontal offset
            else
              userColOffset = 0;  // keep larger vertical offset
          }
        }
        else if (userRowOffset > 0)
        {
          /* Right is empty.  There is a pixel offset trying to move down. */

          if (boardTop[userRow + 1][userCol] != LineEMPTY) // can we go down?
            userRowOffset = 0;    // cancel vertical pixel offset
          else if ((Math.abs(userColOffset) > boardBorderSize)
          || (Math.abs(userRowOffset) > boardBorderSize))
          {
            /* Rounding a corner, select one direction over the other. */

            if (Math.abs(userColOffset) > Math.abs(userRowOffset))
              userRowOffset = 0;  // keep larger horizontal offset
            else
              userColOffset = 0;  // keep larger vertical offset
          }
        }
      }

      /* The complex corner cases have already been done.  Now check for the
      simple vertical-only cases. */

      if (userRowOffset < 0)
      {
        /* There is a pixel offset trying to move up. */

        if (boardTop[userRow][userCol] != LineEMPTY)
          userRowOffset = 0;      // cancel vertical pixel offset
      }
      else if (userRowOffset > 0)
      {
        /* There is a pixel offset trying to move down. */

        if (boardTop[userRow + 1][userCol] != LineEMPTY)
          userRowOffset = 0;      // cancel vertical pixel offset
      }

      /* Did the user reach the exit?  If so, then announce the success.  If
      not, just draw or redraw the user's position normally. */

      if (boardDistance[userRow][userCol] < 2)
      {
        gameState = GameFINISH;   // no more moves allowed
        messageText.setText("You're there!  You reached the exit!  Click \"New Game\" to play again.");
        userColOffset = userRowOffset = 0; // cancel any pixel position offsets
        if (winSound != null)     // if we were able to load winning sound clip
          winSound.play();        // play this sample sound from the Java SDK
        boardPaint(gr);           // shows everything since state = GameFINISH
        flashBoardUser(gr, userRow, userCol, userRowOffset, userColOffset);
      }
      else
      {
        /* Draw the user's new board position by copying pixels from the old
        rectangular area to the new location.  Then erase anything left behind.
        This avoids display flicker caused by erasing and redrawing something
        many times. */

        userNewX = (userCol * boardGridSize) + boardBorderSize
          + boardLineWidth + canvasBorder + userColOffset;
        userNewY = (userRow * boardGridSize) + boardBorderSize
          + boardLineWidth + canvasBorder + userRowOffset;
        hz = userNewX - userOldX; // horizontal displacement
        vt = userNewY - userOldY; // vertical displacement
        if ((hz != 0) || (vt != 0))
        {
          gr.copyArea(userOldX, userOldY, boardSymbolSize, boardSymbolSize,
            hz, vt);
          gr.setColor(BACKGROUND); // color to erase piece of old location

          if ((Math.abs(hz) >= boardSymbolSize)
          || (Math.abs(vt) >= boardSymbolSize))
          {
            /* The old and new rectangles don't overlap, so we can just clear
            the old location to the background color.  This will happen
            occasionally with really wild mouse movements, such as when the
            user moves the mouse up to the control buttons. */

            gr.fillRect(userOldX, userOldY, boardSymbolSize, boardSymbolSize);
          }
          else
          {
            /* Old and new rectangles overlap.  Clear edges in the opposite
            direction from the movement. */

            if (hz < 0)           // move left means clean right
              gr.fillRect(userNewX + boardSymbolSize, userOldY, -hz,
                boardSymbolSize);
            else if (hz > 0)      // move right means clean left
              gr.fillRect(userOldX, userOldY, hz, boardSymbolSize);

            if (vt < 0)           // move up means clean down
              gr.fillRect(userOldX, userNewY + boardSymbolSize,
                boardSymbolSize, -vt);
            else if (vt > 0)      // move down means clean up
              gr.fillRect(userOldX, userOldY, boardSymbolSize, vt);
          }
        }
        else
        {
          /* User didn't change position, so nothing to move or redraw. */
        }

        /* Moving to a new position may reveal some lines that were previously
        hidden. */

        makeLinesVisible(gr, userRow, userCol, true);
      }
      gr.dispose();               // release graphics context
    }
  } // end of boardMouseMoved() method


/*
  boardPaint() method

  This method is called to redraw the entire game board.  It is called by our
  dummy Canvas class (MazeFog2Board), boardMouseMoved(), keyPressed(), and by
  showExitPath().

  Paint() methods are called when a window is first created, gets resized, or
  needs to be redrawn after being overwritten.  We can't assume that anything
  we already drew is still on the display.
 */
  void boardPaint(
    Graphics gr)                  // graphics context for <boardCanvas>
  {
    int boardHeight;              // height (in pixels) of actual game board
    int boardWidth;               // width (in pixels) of actual game board
    int col;                      // temporary column number (index)
    int row;                      // temporary row number (index)

    /* Erase the canvas to have our desired background color. */

    boardHeight = boardCanvas.getSize().height; // for JDK1.1
    boardWidth = boardCanvas.getSize().width; // for JDK1.1
    gr.setColor(BACKGROUND);      // clear board to background color
    gr.fillRect(0, 0, boardWidth, boardHeight);

    /* If the game board has been created, then display visible board lines and
    special board squares. */

    if (boardDistance != null)      // is there a game board?
    {
      /* Display the visible horizontal lines. */

      for (row = 0; row <= numRows; row ++)
        for (col = 0; col < numCols; col ++)
        {
          switch (boardTop[row][col])
          {
            case LineEMPTY:       // empty line, do nothing
              break;

            case LineHIDDEN:      // hidden line
              if (gameState != GameACTIVE)
                drawBoardTopLine(gr, row, col, ColorLINE);
              break;

            case LineVISIBLE:     // visible line
              drawBoardTopLine(gr, row, col, ColorLINE);
              break;

            default:
              System.out.println("error in boardPaint(): bad boardTop["
                + row + "][" + col + "] = " + boardTop[row][col]);
          }
        }

      /* Display the visible vertical lines. */

      for (row = 0; row < numRows; row ++)
        for (col = 0; col <= numCols; col ++)
        {
          switch (boardLeft[row][col])
          {
            case LineEMPTY:       // empty line, do nothing
              break;

            case LineHIDDEN:      // hidden line
              if (gameState != GameACTIVE)
                drawBoardLeftLine(gr, row, col, ColorLINE);
              break;

            case LineVISIBLE:        // visible line
              drawBoardLeftLine(gr, row, col, ColorLINE);
              break;

            default:
              System.out.println("error in boardPaint(): bad boardLeft["
                + row + "][" + col + "] = " + boardLeft[row][col]);
          }
        }

      /* Display special board squares. */

      drawBoardExit(gr, exitRow, exitCol); // mark exit on game board
      drawBoardUser(gr, userRow, userCol, userRowOffset, userColOffset,
        ColorUSER);               // mark user's position
    }
  } // end of boardPaint() method


/*
  doBiggerButton() method

  The user clicked the "Bigger" button for bigger board squares, or typed an
  equivalent keyboard mnemonic.
*/
  void doBiggerButton()
  {
    if (sizeIndex < (sizeList.length - 1))
      sizeIndex ++;               // go to a bigger size, if there is one
    doNewgameButton();            // and start a new game

  } // end of doBiggerButton() method


/*
  doNewgameButton() method

  The user clicked the "New Game" button to start a new game, or typed an
  equivalent keyboard mnemonic.
*/
  void doNewgameButton()
  {
    setBoardSizes();              // set sizes of board elements
    makeBoard(true);              // make maze, draw lines as we create
    boardCanvas.repaint();        // redraw the game board
    boardCanvas.requestFocus();   // set focus so we can listen for arrow keys
    gameState = GameACTIVE;       // let the game begin
    messageText.setText(noMessage); // clear any previous message text

  } // end of doNewgameButton() method


/*
  doShowmeButton() method

  The user clicked the "Show Me" button to see the solution, or typed an
  equivalent keyboard mnemonic.
*/
  void doShowmeButton()
  {
    gameState = GameFINISH;       // not allowed to move after seeing solution
    showExitPath();               // animate the path to the exit
    boardCanvas.requestFocus();   // set focus so we can listen for arrow keys
    messageText.setText("Click the \"New Game\" button to play again.");

  } // end of doShowmeButton() method


/*
  doSmallerButton() method

  The user clicked the "Smaller" button for smaller board squares, or typed an
  equivalent keyboard mnemonic.
*/
  void doSmallerButton()
  {
    if (sizeIndex > 0)
      sizeIndex --;               // go to a smaller size, if there is one
    doNewgameButton();            // and start a new game

  } // end of doSmallerButton() method


/*
  drawBoardExit() method

  Draw the exit or goal for the maze, at the given board position.  Currently,
  this is done with alternating blue and white boxes.  Anything else is really
  difficult to draw at small sizes.
*/
  void drawBoardExit(
    Graphics gr,                  // graphics context of <boardCanvas>
    int row,                      // row number (index)
    int col)                      // column number (index)
  {
    boolean colorFlag;            // flips between two colors
    int hz;                       // temporary number of horizontal pixels
    int size;                     // remaining box size
    int vt;                       // temporary number of vertical pixels
    int width;                    // width of current box line

    colorFlag = true;             // start with first color
    size = boardSymbolSize + (2 * boardBorderSize); // initial box size
    hz = (col * boardGridSize) + boardLineWidth + canvasBorder; // x position
    vt = (row * boardGridSize) + boardLineWidth + canvasBorder; // y position
    while (size > 0)
    {
      width = colorFlag ? 2 : 3;  // different width for each color
      gr.setColor(colorFlag ? ColorEXIT1 : ColorEXIT2); // select clor
      gr.fillRect(hz, vt, size, size); // draw box
      colorFlag = ! colorFlag;    // flip color flag
      hz += width;                // next x position
      vt += width;                // next y position
      size -= (width * 2);        // next box size
    }
  } // end of drawBoardExit() method


/*
  drawBoardLeftLine() method

  Draw a left (vertical) line on the game board at the given board position.
*/
  void drawBoardLeftLine(
    Graphics gr,                  // graphics context of <boardCanvas>
    int row,                      // row number (index)
    int col,                      // column number (index)
    Color shade)                  // <BACKGROUND> or <ColorLINE>
  {
    gr.setColor(shade);           // switch to caller's favorite color
    gr.fillRect(                  // draw line as filled rectangle
      (col * boardGridSize) + canvasBorder,
      (row * boardGridSize) + canvasBorder,
      boardLineWidth, (boardGridSize + boardLineWidth));

  } // end of drawBoardLeftLine() method


/*
  drawBoardTopLine() method

  Draw a top (horizontal) line on the game board at the given board position.
*/
  void drawBoardTopLine(
    Graphics gr,                  // graphics context of <boardCanvas>
    int row,                      // row number (index)
    int col,                      // column number (index)
    Color shade)                  // <BACKGROUND> or <ColorLINE>
  {
    gr.setColor(shade);           // switch to caller's favorite color
    gr.fillRect(                  // draw line as filled rectangle
      (col * boardGridSize) + canvasBorder,
      (row * boardGridSize) + canvasBorder,
      (boardGridSize + boardLineWidth), boardLineWidth);

  } // end of drawBoardTopLine() method


/*
  drawBoardUser() method

  Draw the user's position as a blue circle.  The caller gives us row and
  column numbers, plus pixel row and column offsets.  We assume that the
  offsets are valid, and drawing in that location won't overlap something like
  a wall!
*/
  void drawBoardUser(
    Graphics gr,                  // graphics context of <boardCanvas>
    int row,                      // row number (index)
    int col,                      // column number (index)
    int rowOffset,                // row offset (y coordinate) in pixels
    int colOffset,                // column offset (x coordinate) in pixels
    Color shade)                  // <BACKGROUND> or <ColorUSER>
  {
    gr.setColor(shade);           // switch to caller's favorite color
    gr.fillOval(
      (col * boardGridSize) + boardBorderSize + boardLineWidth + canvasBorder
        + colOffset,
      (row * boardGridSize) + boardBorderSize + boardLineWidth + canvasBorder
        + rowOffset,
      boardSymbolSize, boardSymbolSize);

  } // end of drawBoardUser() method


/*
  flashBoardUser() method

  Redraw the user's position several times in different colors, ending with the
  standard color.
*/
  void flashBoardUser(
    Graphics gr,                  // graphics context of <boardCanvas>
    int row,                      // row number (index)
    int col,                      // column number (index)
    int rowOffset,                // row offset (y coordinate) in pixels
    int colOffset)                // column offset (x coordinate) in pixels
  {
    drawBoardUser(gr, row, col, rowOffset, colOffset, Color.black);   // 000
    sleep(DelayFLASH);
    drawBoardUser(gr, row, col, rowOffset, colOffset, Color.green);   // 010
    sleep(DelayFLASH);
    drawBoardUser(gr, row, col, rowOffset, colOffset, Color.cyan);    // 011
    sleep(DelayFLASH);
    drawBoardUser(gr, row, col, rowOffset, colOffset, Color.blue);    // 001
    sleep(DelayFLASH);
    drawBoardUser(gr, row, col, rowOffset, colOffset, Color.magenta); // 101
    sleep(DelayFLASH);
    drawBoardUser(gr, row, col, rowOffset, colOffset, Color.red);     // 100
    sleep(DelayFLASH);
    drawBoardUser(gr, row, col, rowOffset, colOffset, Color.yellow);  // 110
    sleep(DelayFLASH);
    drawBoardUser(gr, row, col, rowOffset, colOffset, Color.white);   // 111
    sleep(DelayFLASH);
    drawBoardUser(gr, row, col, rowOffset, colOffset, ColorUSER); // standard

  } // end of flashBoardUser() method


/*
  keyPressed(), keyReleased(), keyTyped() methods

  We listen to arrow keys with the keyPressed() method, because arrow keys are
  not fully-formed Unicode characters.  We listen to keyboard mnemonics (for
  the control buttons) with keyTyped() since they are Unicode characters.  The
  keyReleased() method is just to complete the KeyListener interface.

  The arrow keys move in whole increments of one square (rows and columns);
  they don't use the pixel offsets like the mouse movement.  Hence, the code in
  keyPressed() is easier to understand than in boardMouseMoved().
*/
  public void keyPressed(KeyEvent event)
  {
    Graphics gr;                  // graphics context for <boardCanvas>

    if (gameState == GameACTIVE)  // ignore keys unless there is an active game
    {
      /* Allocate a graphics context for <boardCanvas> and erase the previous
      user position, even if we don't end up moving. */

      gr = boardCanvas.getGraphics(); // get graphics context
      drawBoardUser(gr, userRow, userCol, userRowOffset, userColOffset,
        BACKGROUND);
      userColOffset = userRowOffset = 0; // cancel any pixel position offsets

      /* Sort out which key was pressed. */

      switch (event.getKeyCode())
      {
        case KeyEvent.VK_D:       // "D" key for "down"
        case KeyEvent.VK_DOWN:    // down arrow
        case KeyEvent.VK_KP_DOWN: // numeric keypad down arrow
          if (boardTop[userRow + 1][userCol] == LineEMPTY)
          {
            userRow ++;           // move down
          }
          break;

        case KeyEvent.VK_L:       // "L" key for "left"
        case KeyEvent.VK_LEFT:    // left arrow
        case KeyEvent.VK_KP_LEFT: // numeric keypad left arrow
          if (boardLeft[userRow][userCol] == LineEMPTY)
          {
            userCol --;           // move left
          }
          break;

        case KeyEvent.VK_R:       // "R" key for "right"
        case KeyEvent.VK_RIGHT:   // right arrow
        case KeyEvent.VK_KP_RIGHT: // numeric keypad right arrow
          if (boardLeft[userRow][userCol + 1] == LineEMPTY)
          {
            userCol ++;           // move right
          }
          break;

        case KeyEvent.VK_U:       // "U" key for "up"
        case KeyEvent.VK_UP:      // up arrow
        case KeyEvent.VK_KP_UP:   // numeric keypad up arrow
          if (boardTop[userRow][userCol] == LineEMPTY)
          {
            userRow --;           // move up
          }
          break;

        default:                  // ignore all other keys
          break;
      }

      /* Did the user reach the exit?  If so, then announce the success.  If
      not, just draw or redraw the user's position normally. */

      if (boardDistance[userRow][userCol] < 2)
      {
        gameState = GameFINISH;   // no more moves allowed
        messageText.setText("You're there!  You reached the exit!  Click \"New Game\" to play again.");
        if (winSound != null)     // if we were able to load winning sound clip
          winSound.play();        // play this sample sound from the Java SDK
        boardPaint(gr);           // shows everything since state = GameFINISH
        flashBoardUser(gr, userRow, userCol, userRowOffset, userColOffset);
      }
      else
      {
        drawBoardUser(gr, userRow, userCol, userRowOffset, userColOffset,
          ColorUSER);             // draw user normally
        makeLinesVisible(gr, userRow, userCol, true);
                                  // this may reveal hidden lines
      }
      gr.dispose();               // release graphics context
    }
  } // end of keyPressed() method

  public void keyReleased(KeyEvent event) { }

  public void keyTyped(KeyEvent event)
  {
    switch (event.getKeyChar())   // which character did user type on keyboard?
    {
      case ('+'):
      case ('B'):                 // "B" for "bigger"
      case ('b'):
        doBiggerButton();         // "Bigger" button for bigger board squares
        break;

      case ('G'):                 // "G" for "game"
      case ('g'):
      case ('N'):                 // "N" for "new"
      case ('n'):
        doNewgameButton();        // "New Game" button to start a new game
        break;

      case ('H'):                 // "H" for "help"
      case ('h'):
      case ('M'):                 // "M" for "me"
      case ('m'):
      case ('Q'):                 // "Q" for "quit"
      case ('q'):
        doShowmeButton();         // "Show Me" button to see the solution
        break;

      case ('-'):
      case ('S'):                 // "S" for "smaller"
      case ('s'):
        doSmallerButton();        // "Smaller" button for smaller board squares
        break;

      default:                    // ignore all other keys
        break;
    }
  } // end of keyTyped() method


/*
  makeBoard() method

  Make the game board.  That is, draw the maze.  We do this by randomly picking
  a square along one of the four edges.  This square becomes the exit or goal
  for the game.  Then starting from this square, we "tunnel" into an array
  whose values indicate if a square is empty, or in use.  Empty squares can be
  added to our current tunnel; occupied squares stop us and force us to
  backtrack.  Directions and distances are chosen randomly.

  This method usually, but not always, completes the game board in the sense
  that it uses every available position.  The incomplete sections just look
  like empty "fog" to the user, which can be more confusing than a totally
  complete maze!  The created maze is simply connected: there are no loops.
  While drawing a maze requires a lot of pseudo-random activity, the algorithm
  should not be chosen randomly.

  The only argument to this method is a display flag.  If true, then we draw
  the maze as we create it.  If false, then we create the maze without drawing
  and without delays (calls to the sleep() method).  Most web browsers don't
  display anything until after the init() method returns, so there is no point
  in having an animated introduction unless it runs in a separate thread.  The
  user just sits there waiting and thinking that the program is really slow.
*/
  void makeBoard(
    boolean displayFlag)          // true if we draw lines as we create maze
  {
    int boardHeight;              // height (in pixels) of actual game board
    int boardWidth;               // width (in pixels) of actual game board
    int col;                      // temporary column number (index)
    Graphics gr;                  // graphics context for <boardCanvas>
    int row;                      // temporary row number (index)

    /* Get a graphics context for drawing on the game board. */

    if (displayFlag)
      gr = boardCanvas.getGraphics();
    else
      gr = null;                  // to keep compiler happy

    /* Calculate the number of rows and columns for the game board, knowing the
    size of <boardCanvas> in pixels and the size of each board square. */

    boardHeight = boardCanvas.getSize().height; // for JDK1.1
    boardWidth = boardCanvas.getSize().width; // for JDK1.1
    if (displayFlag)
    {
      gr.setColor(BACKGROUND);    // clear board to background color
      gr.fillRect(0, 0, boardWidth, boardHeight);
    }

    numRows = (boardHeight - (2 * canvasBorder) - boardLineWidth)
      / boardGridSize;
    numRows = Math.max(3, numRows); // minimum of three rows

    numCols = (boardWidth - (2 * canvasBorder) - boardLineWidth)
      / boardGridSize;
    numCols = Math.max(3, numCols); // minimum of three columns

    /* Allocate new arrays.  Note that the internal game board has an extra
    column on the right and an extra row on the bottom to make programming
    logic easier. */

    boardDistance = new int[numRows + 1][numCols + 1]; // distance from exit
    boardLeft = new int[numRows + 1][numCols + 1]; // vertical lines
    boardTop = new int[numRows + 1][numCols + 1]; // horizontal lines

    /* Initialize the arrays to default values. */

    for (row = 0; row <= numRows; row ++)
      for (col = 0; col <= numCols; col ++)
      {
        boardDistance[row][col] = -1; // invalidate all distances from exit
        boardLeft[row][col] = LineEMPTY; // no vertical lines yet
        boardTop[row][col] = LineEMPTY; // no horizontal lines yet
      }
    exitCol = exitRow = 0;        // just to be safe, set some initial value
    startCol = startDistance = startRow = 0;
    userCol = userColOffset = userRow = userRowOffset = 0;

    /* Pick a random edge (top, left, right, bottom) and a random square on
    that edge to become the exit ... which is our starting point.  By this
    method, corners have a double chance of being selected. */

    switch ((int) (Math.random() * 4))
    {
      case 0:                     // top edge
        row = 0;                  // first row
        col = (int) (Math.random() * numCols); // random column
        boardLeft[row][col] = LineHIDDEN;     // left line
        boardLeft[row][col + 1] = LineHIDDEN; // right line
        boardTop[row + 1][col] = LineHIDDEN;  // bottom line
        if (displayFlag)
        {
          drawBoardLeftLine(gr, row, col, ColorLINE); // left line
          drawBoardLeftLine(gr, row, col + 1, ColorLINE); // right line
          drawBoardTopLine(gr, row + 1, col, ColorLINE); // bottom line
        }
        break;

      case 1:                     // left edge
        row = (int) (Math.random() * numRows); // random row
        col = 0;                  // first column
        boardTop[row][col] = LineHIDDEN;      // top line
        boardLeft[row][col + 1] = LineHIDDEN; // right line
        boardTop[row + 1][col] = LineHIDDEN;  // bottom line
        if (displayFlag)
        {
          drawBoardTopLine(gr, row, col, ColorLINE); // top line
          drawBoardLeftLine(gr, row, col + 1, ColorLINE); // right line
          drawBoardTopLine(gr, row + 1, col, ColorLINE); // bottom line
        }
        break;

      case 2:                     // right edge
        row = (int) (Math.random() * numRows); // random row
        col = numCols - 1;        // last column
        boardTop[row][col] = LineHIDDEN;      // top line
        boardLeft[row][col] = LineHIDDEN;     // left line
        boardTop[row + 1][col] = LineHIDDEN;  // bottom line
        if (displayFlag)
        {
          drawBoardTopLine(gr, row, col, ColorLINE); // top line
          drawBoardLeftLine(gr, row, col, ColorLINE); // left line
          drawBoardTopLine(gr, row + 1, col, ColorLINE); // bottom line
        }
        break;

      default:                    // bottom edge
        row = numRows - 1;        // last row
        col = (int) (Math.random() * numCols); // random column
        boardTop[row][col] = LineHIDDEN;      // top line
        boardLeft[row][col] = LineHIDDEN;     // left line
        boardLeft[row][col + 1] = LineHIDDEN; // right line
        if (displayFlag)
        {
          drawBoardTopLine(gr, row, col, ColorLINE); // top line
          drawBoardLeftLine(gr, row, col, ColorLINE); // left line
          drawBoardLeftLine(gr, row, col + 1, ColorLINE); // right line
        }
        break;
    }

    /* Remember where the exit is and mark the exit on the game board. */

    exitRow = row;                // save exit (goal) row number
    exitCol = col;                // save exit (goal) column number
    boardDistance[exitRow][exitCol] = 0; // exit is zero squares from itself
    if (displayFlag)
      drawBoardExit(gr, exitRow, exitCol); // mark exit on game board

    /* Complete the maze by recursion. */

    makeBoardRecurse(gr, exitRow, exitCol, displayFlag);

    /* Draw the user's position, which is the furthest from the exit. */

    userRow = startRow;           // set current position to starting position
    userCol = startCol;
    if (displayFlag)
      drawBoardUser(gr, userRow, userCol, userRowOffset, userColOffset,
        ColorUSER);               // mark user's position
    makeLinesVisible(gr, userRow, userCol, displayFlag);
                                  // this may reveal hidden lines

    /* Release the graphics context, because it's better to explicitly release
    the graphics context rather than wait for the garbage collector. */

    if (displayFlag)
      gr.dispose();               // release graphics context

  } // end of makeBoard() method


/*
  makeBoardRecurse() method

  The maze is created by recursively calling this method.  Each call has a
  starting row and column number.  We attempt to move in all four directions,
  in random order and with random distances.  Then we recurse with the end
  points of the successful attempts.  The recursion happens after our four
  attempts, because recursing on each attempt would generate too many spiral
  paths and not enough branches.
*/
  void makeBoardRecurse(
    Graphics gr,                  // graphics context of <boardCanvas>
    int fromRow,                  // recurse from this row number (index)
    int fromCol,                  // recurse from this column number (index)
    boolean displayFlag)          // true if we draw lines as we create maze
  {
    int attempt;                  // how far we want to move
    int branchCol;                // column number of branching point
    int branchRow;                // row number of branching point
    int direction;                // current direction
    int dirIncr;                  // pseudo-random increment for next direction
    int good;                     // number of good paths found
    int goodCols[];               // column numbers for good paths
    int goodRows[];               // row numbers for good paths
    int thisCol;                  // column number of current position
    int thisRow;                  // row number of current position

    /* Clear the list of known good paths (successful attempts). */

    good = 0;                     // nothing found yet
    goodCols = new int[4];        // four directions maximum size
    goodRows = new int[4];

    /* While we attempt to move in four directions, we don't make all of those
    moves from the caller's starting position.  The second and later directions
    choose a branching point off the previous path. */

    branchCol = thisCol = fromCol; // everyone starts in the same place
    branchRow = thisRow = fromRow;

    /* Choose the first direction at random, and choose a random increment
    between directions.  We pretend that there are five directions and ignore
    the fifth direction, because five is relatively prime to 1, 2, 3, and 4.
    This gives us four possible increments with one fake (wasted) direction.
    If we used only the four real directions, we would only have two possible
    increments (1 and 3) since 2 is not relatively prime to 4. */

    direction = (int) (Math.random() * 5); // initial direction from 0 to 4
    dirIncr = 1 + (int) (Math.random() * 4); // increment from 1 to 4

    for (int i = 0; i < 5; i ++)  // try each of "five" directions
    {
      /* Try to go a random distance in the given direction.  Start somewhere
      along the path between where we are now and where we last branched off. */

      branchRow = thisRow = branchRow
        + (int) (Math.random() * (thisRow - branchRow));
      branchCol = thisCol = branchCol
        + (int) (Math.random() * (thisCol - branchCol));
      attempt = 2 + (int) (Math.random() * 5); // attempted distance

      while (attempt > 0)
      {
        switch (direction)
        {
          case 0:                 // can we go up?
            if ((thisRow < 1)
            || (boardDistance[thisRow - 1][thisCol] >= 0))
            {
              attempt = 0;        // can't go up
            }
            else
            {
              thisRow --;         // go up
              boardTop[thisRow + 1][thisCol] = LineEMPTY; // remove old top
              boardTop[thisRow][thisCol] = LineHIDDEN; // add new top line
              boardLeft[thisRow][thisCol] = LineHIDDEN; // add new left line
              boardLeft[thisRow][thisCol + 1] = LineHIDDEN; // add new right line
              if (displayFlag)
              {
                drawBoardTopLine(gr, thisRow + 1, thisCol, BACKGROUND); // old top
                drawBoardTopLine(gr, thisRow, thisCol, ColorLINE); // new top
                drawBoardLeftLine(gr, thisRow, thisCol, ColorLINE); // new left
                drawBoardLeftLine(gr, thisRow, thisCol + 1, ColorLINE); // new right
              }
            }
            break;

          case 1:                 // can we go left?
            if ((thisCol < 1)
            || (boardDistance[thisRow][thisCol - 1] >= 0))
            {
              attempt = 0;        // can't go left
            }
            else
            {
              thisCol --;         // go left
              boardLeft[thisRow][thisCol + 1] = LineEMPTY; // remove old left
              boardTop[thisRow][thisCol] = LineHIDDEN; // add new top line
              boardLeft[thisRow][thisCol] = LineHIDDEN; // add new left line
              boardTop[thisRow + 1][thisCol] = LineHIDDEN; // add new bottom line
              if (displayFlag)
              {
                drawBoardLeftLine(gr, thisRow, thisCol + 1, BACKGROUND); // old left
                drawBoardTopLine(gr, thisRow, thisCol, ColorLINE); // new top
                drawBoardLeftLine(gr, thisRow, thisCol, ColorLINE); // new left
                drawBoardTopLine(gr, thisRow + 1, thisCol, ColorLINE); // new bottom
              }
            }
            break;

          case 2:                 // can we go right?
            if ((thisCol >= (numCols - 1))
            || (boardDistance[thisRow][thisCol + 1] >= 0))
            {
              attempt = 0;        // can't go right
            }
            else
            {
              thisCol ++;         // go right
              boardLeft[thisRow][thisCol] = LineEMPTY; // remove old right
              boardTop[thisRow][thisCol] = LineHIDDEN; // add new top line
              boardLeft[thisRow][thisCol + 1] = LineHIDDEN; // add new right line
              boardTop[thisRow + 1][thisCol] = LineHIDDEN; // add new bottom line
              if (displayFlag)
              {
                drawBoardLeftLine(gr, thisRow, thisCol, BACKGROUND); // old right
                drawBoardTopLine(gr, thisRow, thisCol, ColorLINE); // new top
                drawBoardLeftLine(gr, thisRow, thisCol + 1, ColorLINE); // new right
                drawBoardTopLine(gr, thisRow + 1, thisCol, ColorLINE); // new bottom
              }
            }
            break;

          case 3:                 // can we go down?
            if ((thisRow >= (numRows - 1))
            || (boardDistance[thisRow + 1][thisCol] >= 0))
            {
              attempt = 0;        // can't go down
            }
            else
            {
              thisRow ++;         // go down
              boardTop[thisRow][thisCol] = LineEMPTY; // remove old bottom
              boardLeft[thisRow][thisCol] = LineHIDDEN; // add new left line
              boardLeft[thisRow][thisCol + 1] = LineHIDDEN; // add new right line
              boardTop[thisRow + 1][thisCol] = LineHIDDEN; // add new bottom line
              if (displayFlag)
              {
                drawBoardTopLine(gr, thisRow, thisCol, BACKGROUND); // old bottom
                drawBoardLeftLine(gr, thisRow, thisCol, ColorLINE); // new left
                drawBoardLeftLine(gr, thisRow, thisCol + 1, ColorLINE); // new right
                drawBoardTopLine(gr, thisRow + 1, thisCol, ColorLINE); // new bottom
              }
            }
            break;

          default:                // fake direction to make a total of five
            attempt = 0;          // ignore this direction
            break;
        }

        /* If we were able to move, then mark the new square.  We also remember
        the board square that is the maximum distance from the exit or goal,
        because that is where we start the user. */

        if (attempt > 0)          // did we go anywhere?
        {
          boardDistance[thisRow][thisCol] = boardDistance[branchRow][branchCol]
            + Math.abs(branchRow - thisRow) + Math.abs(branchCol - thisCol);
          if (boardDistance[thisRow][thisCol] > startDistance)
          {
            startRow = thisRow;   // save position with maximum distance
            startCol = thisCol;
            startDistance = boardDistance[thisRow][thisCol];
          }

          attempt --;             // reduce remaining distance to try
          if (displayFlag)        // are we displaying the maze as we create?
            sleep(DelayDRAW);     // yes, then delay briefly for animation
        }
        else
          break;                  // went nowhere, exit from while loop
      }

      /* If this direction went somewhere, then remember the end point. */

      if ((branchCol != thisCol) || (branchRow != thisRow))
      {
        goodCols[good] = thisCol;
        goodRows[good] = thisRow;
        good ++;
      }

      /* Get the next pseudo-random direction to try. */

      direction = (direction + dirIncr) % 5;
    }

    /* Call ourself recursively if we went anywhere. */

    for (int i = 0; i < good; i ++)
      makeBoardRecurse(gr, goodRows[i], goodCols[i], displayFlag);

  } // end of makeBoardRecurse() method


/*
  makeLinesVisible() method

  If there are any lines near a given position that are currently hidden, then
  make those lines visible.  This clears the "fog" that the user goes through
  as he/she traverses the maze.
*/
  void makeLinesVisible(
    Graphics gr,                  // graphics context of <boardCanvas>
    int fromRow,                  // reveal from this row number (index)
    int fromCol,                  // reveal from this column number (index)
    boolean displayFlag)          // true if we draw changed lines
  {
    int col;                      // temporary column number (index)
    int row;                      // temporary row number (index)

    /* Reveal left (vertical) lines. */

    for (row = (fromRow - 1); row <= (fromRow + 1); row ++)
      for (col = (fromCol - 1); col <= (fromCol + 2); col ++)
      {
        if ((row >= 0) && (row < numRows) && (col >= 0) && (col <= numCols)
        && (boardLeft[row][col] == LineHIDDEN))
        {
          boardLeft[row][col] = LineVISIBLE;
          if (displayFlag)
            drawBoardLeftLine(gr, row, col, ColorLINE);
        }
      }

    /* Reveal top (horizontal) lines. */

    for (row = (fromRow - 1); row <= (fromRow + 2); row ++)
      for (col = (fromCol - 1); col <= (fromCol + 1); col ++)
      {
        if ((row >= 0) && (row <= numRows) && (col >= 0) && (col < numCols)
        && (boardTop[row][col] == LineHIDDEN))
        {
          boardTop[row][col] = LineVISIBLE;
          if (displayFlag)
            drawBoardTopLine(gr, row, col, ColorLINE);
        }
      }

  } // end of makeLinesVisible() method


/*
  paint() method

  This applet doesn't have paint() or update() methods because all drawing is
  done by components (Button, Canvas, Panel, etc).
*/


/*
  setBoardSizes() method

  The caller has set <sizeIndex> to select the size of the board squares from
  the <sizeList> array.  Change the sizes of other board elements to match.
*/
  void setBoardSizes()
  {
    boardGridSize = sizeList[sizeIndex]; // initial size of each board square

    boardLineWidth = (int) (boardGridSize * 0.10); // width of board lines
    boardLineWidth = Math.max(1, boardLineWidth); // minimum of one pixel

    boardSymbolSize = (int) (boardGridSize * 0.70); // size of board symbols
    boardSymbolSize = Math.max(3, boardSymbolSize); // minimum of three pixels

    boardBorderSize = (boardGridSize - boardLineWidth - boardSymbolSize) / 2;
                                  // inner border between lines and symbols
    boardBorderSize = Math.max(1, boardBorderSize); // minimum of one pixel

    boardGridSize = (boardBorderSize * 2) + boardLineWidth + boardSymbolSize;
                                  // final adjusted size of each board square

  } // end of setBoardSizes() method


/*
  showExitPath() method

  Show the user the path to the exit.  After we do this, the game is over.
*/
  void showExitPath()
  {
    int col;                      // temporary column number (index)
    int delay;                    // delay between positions for animation
    Graphics gr;                  // graphics context for <boardCanvas>
    int row;                      // temporary row number (index)

    /* Check if we were called incorrectly.  This shouldn't happen once the
    program is finished, but may happen during debugging. */

    if (gameState != GameFINISH)
    {
      System.out.println("error in showExitPath(): bad gameState = "
        + gameState);
    }

    /* Get a graphics context for drawing on the game board. */

    gr = boardCanvas.getGraphics();

    /* Redraw the game board, showing all lines including the hidden lines. */

    userColOffset = userRowOffset = 0; // cancel any pixel position offsets
    boardPaint(gr);               // shows everything since state = GameFINISH

    /* Where do we begin?  From the user's current position, if not near the
    exit.  From the original starting position, if the user has already solved
    the maze. */

    if (boardDistance[userRow][userCol] > 1)
    {
      row = userRow;              // start from current position
      col = userCol;
    }
    else
    {
      row = startRow;             // go back to original starting position
      col = startCol;
    }

    /* How long should we delay between positions while animating?  Depends
    upon how far away from the exit we are.  Allow about 3000 milliseconds (ms,
    or three seconds) for drawing the path. */

    delay = 3000 / (boardDistance[row][col] + 2); // +2 avoids divide by zero
    if (delay < 10)
      delay = 10;                 // enforce a minimum delay of 10 ms
    else if (delay > 200)
      delay = 200;                // enforce a maximum delay of 200 ms

    /* The path to the exit is found by (1) not walking through walls, and (2)
    decreasing the <boardDistance> by one at each step. */

    while (boardDistance[row][col] > 0)
    {
      drawBoardUser(gr, row, col, 0, 0, ColorUSER);
                                  // draw user at each position on path
      sleep(delay);               // delay for animation

      /* Check if moving up is closer to the exit. */

      if ((row > 0)
      && (boardTop[row][col] == LineEMPTY)
      && (boardDistance[row - 1][col] < boardDistance[row][col]))
      {
        row --;                   // go up
      }

      /* Check if moving left is closer to the exit. */

      else if ((col > 0)
      && (boardLeft[row][col] == LineEMPTY)
      && (boardDistance[row][col - 1] < boardDistance[row][col]))
      {
        col --;                   // go left
      }

      /* Check if moving right is closer to the exit. */

      else if ((col < (numCols - 1))
      && (boardLeft[row][col + 1] == LineEMPTY)
      && (boardDistance[row][col + 1] < boardDistance[row][col]))
      {
        col ++;                   // go right
      }

      /* Check if moving down is closer to the exit. */

      else if ((row < (numRows - 1))
      && (boardTop[row + 1][col] == LineEMPTY)
      && (boardDistance[row + 1][col] < boardDistance[row][col]))
      {
        row ++;                   // go down
      }

      else
      {
        System.out.println("error in showExitPath(): can't find exit at row = "
          + row + " col = " + col);
        break;                    // get out of while loop
      }
    }

    /* Release the graphics context, because it's better to explicitly release
    the graphics context rather than wait for the garbage collector. */

    gr.dispose();                 // release graphics context

  } // end of showExitPath() method


/*
  sleep() method

  Sleep (delay) for the given number of milliseconds.  If this method is called
  from the regular GUI thread, then the GUI thread will be blocked until this
  method returns.  That may be acceptable for short delays (during animation)
  but not long delays of several seconds.

  The delay must be at least 5 ms for Netscape 4.7/4.8 (JDK1.1).  Anything less
  is treated as no delay.
*/
  void sleep(int delay)
  {
    try { Thread.sleep(delay); }  // sleep (delay)
        catch (InterruptedException except) { /* do nothing */ }

  } // end of sleep() method


/*
  update() method

  This applet doesn't have paint() or update() methods because all drawing is
  done by components (Button, Canvas, Panel, etc).
*/

} // end of MazeFog2 class

// ------------------------------------------------------------------------- //

/*
  MazeFog2Board class

  Create a subclass of Canvas for the game board, so that we can take over from
  the regular mouse and paint routines.  These Canvas methods pass back their
  arguments to methods in MazeFog2 so that they can be processed in the context
  of the main MazeFog2 class.

  We implement both mouseDragged() and mouseMoved() as the same action, in case
  the user tries to drag his/her board symbol to a new position.  This costs us
  nothing and makes for a friendlier user interface.

  Note that the MazeFog2Board subclass assumes that a MazeFog2 object is the
  parent container of <boardCanvas>.
*/

class MazeFog2Board
      extends Canvas
      implements MouseMotionListener
{
  public void mouseDragged(MouseEvent event)
  {
    ((MazeFog2) this.getParent()).boardMouseMoved(event);
                                  // pass back mouse event
  }

  public void mouseMoved(MouseEvent event)
  {
    ((MazeFog2) this.getParent()).boardMouseMoved(event);
                                  // pass back mouse event
  }

  public void paint(Graphics gr)
  {
    ((MazeFog2) this.getParent()).boardPaint(gr);
                                  // pass back graphics context
  }

} // end of MazeFog2Board class

// ------------------------------------------------------------------------- //

/*
  MazeFog2Window class

  This applet can also be run as an application by calling the main() method
  instead of the init() method.  As an application, it must exit when its main
  window is closed.  A window listener is necessary because EXIT_ON_CLOSE is a
  JFrame option in Java Swing, not a basic AWT Frame option.  It is easier to
  extend WindowAdapter here with one method than to implement all methods of
  WindowListener in the main applet.
*/

class MazeFog2Window extends WindowAdapter
{
  public void windowClosing(WindowEvent event)
  {
    System.exit(0);               // exit from this application
  }
} // end of MazeFog2Window class

/* Copyright (c) 2004 by Keith Fenske.  Released under GNU Public License. */
