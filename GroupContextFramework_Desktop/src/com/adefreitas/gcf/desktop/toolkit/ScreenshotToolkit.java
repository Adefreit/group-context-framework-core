package com.adefreitas.gcf.desktop.toolkit;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class ScreenshotToolkit 
{
	public static File takeScreenshot(int width, int height, String filename)
	{
		try
		{
			//System.out.print("Taking Screenshot (Whole Screen) . . . ");
			
			// Takes a Screenshot of the Primary Monitor
			BufferedImage image = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
			
			// Resizes the Image
			BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = resizedImage.createGraphics();
			g.drawImage(image, 0, 0, width, height, null);
			g.dispose();
			
			// Saves the Image
			File result = new File(filename + ".jpeg");
			ImageIO.write(resizedImage, "jpeg", result);
			
			//System.out.println("DONE!");
			
			return result;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return null;
	}
	
	public static void takeApplicationScreenshot(int topLeftX, int topLeftY, int bottomRightX, int bottomRightY, int width, int height, String filename)
	{
		try
		{
			System.out.print("Taking Screenshot (Coordinates [" + topLeftX + ", " + topLeftY + "] to [" + bottomRightX + ", " + bottomRightY + "]");
			
			// Takes a Screenshot of the Primary Monitor
			BufferedImage image = new Robot().createScreenCapture(new Rectangle(topLeftX, topLeftY, bottomRightX, bottomRightY));
			
			// Resizes the Image
			BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = resizedImage.createGraphics();
			g.drawImage(image, 0, 0, width, height, null);
			g.dispose();
			
			// Saves the Image
			ImageIO.write(resizedImage, "jpeg", new File(filename + ".jpeg"));
			
			System.out.println("DONE!");
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
