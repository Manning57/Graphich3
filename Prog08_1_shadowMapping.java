package jogl_shader_course;

import graphicslib3D.*;
import graphicslib3D.light.*;
import graphicslib3D.GLSLUtils.*;
import graphicslib3D.shape.*;
import jogl_shader_course.Prog04_4_matrixStack.Camera;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.*;
import javax.swing.*;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_CCW;
import static com.jogamp.opengl.GL.GL_CLAMP_TO_EDGE;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL.GL_LINEAR_MIPMAP_LINEAR;
import static com.jogamp.opengl.GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE1;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_CUBE_MAP;
import static com.jogamp.opengl.GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
import static com.jogamp.opengl.GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
import static com.jogamp.opengl.GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
import static com.jogamp.opengl.GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static com.jogamp.opengl.GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
import static com.jogamp.opengl.GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_T;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL2ES2.GL_COMPILE_STATUS;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_LINK_STATUS;
import static com.jogamp.opengl.GL2ES2.GL_TEXTURE_WRAP_R;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
import static com.jogamp.opengl.GL2GL3.GL_TEXTURE_CUBE_MAP_SEAMLESS;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.common.nio.Buffers;

public class Prog08_1_shadowMapping extends JFrame implements GLEventListener {
	private GLCanvas myCanvas;
	private Material thisMaterial;
	private String[] vBlinn1ShaderSource, fBlinn1ShaderSource, vBlinn2ShaderSource, fBlinn2ShaderSource;
	private int rendering_program1, rendering_program2, rendering_program_cube_map;
	private String[] vertShaderSource, fragShaderSource;
	private int vao[] = new int[1];
	private int vbo[] = new int[9];
	private int mv_location, proj_location, vertexLoc, n_location;
	private float aspect;
	private Texture moonTex;
	private Texture earthTex;
	private GLSLUtils util = new GLSLUtils();

	Camera camera = new Camera();

	private Point3D center = new Point3D(0, 0, 0);

	private Sphere mySphere = new Sphere(24);
	private int textureID1, textureID2;

	// earth
	private int earthTexture;
	private Texture joglEarthTexture;

	// mars
	private int marsTexture;
	private Texture joglMarsTexture;

	// location of torus, pyramid and camera
	private Point3D torusLoc = new Point3D(1.6, 0.0, -0.3);
	private Point3D pyrLoc = new Point3D(-1.0, 0.1, 0.3);
	private Point3D sphLoc = new Point3D(0.0, 2.0, 0.0);
	private Point3D cameraLoc = new Point3D(0.0, 0.2, 6.0);
	private Point3D lightLoc = new Point3D(-3.5f, 0f, 0f);
	//private Point3D lightLoc = new Point3D(0f, 0f, 0f);
	private Point3D outerSpace = new Point3D(0f, 0f, -9999f);
	private int toggle = 1;

	private Matrix3D m_matrix = new Matrix3D();
	private Matrix3D v_matrix = new Matrix3D();
	private Matrix3D mv_matrix = new Matrix3D();
	private Matrix3D proj_matrix = new Matrix3D();
	private Matrix3D cubeV_matrix = new Matrix3D();

	// light stuff
	private float[] globalAmbient = new float[] { 1f, 1f, 1f, 1.0f };
	private PositionalLight currentLight = new PositionalLight();
	private float amt = 0.0f;
	private Matrix3D l_matrix = new Matrix3D();

	// shadow stuff
	private int scSizeX, scSizeY;
	private int[] shadow_tex = new int[1];
	private int[] shadow_buffer = new int[1];
	private Matrix3D lightV_matrix = new Matrix3D();
	private Matrix3D lightP_matrix = new Matrix3D();
	private Matrix3D shadowMVP1 = new Matrix3D();
	private Matrix3D shadowMVP2 = new Matrix3D();
	private Matrix3D b = new Matrix3D();

	// model stuff
	private ImportedModel pyramid = new ImportedModel("pyr.obj");
	private Torus myTorus = new Torus(0.6f, 0.4f, 48);
	private int numPyramidVertices, numTorusVertices;

	public Prog08_1_shadowMapping() {
		setTitle("Chapter8 - program 1");
		setSize(800, 800);
		// Making sure we get a GL4 context for the canvas
		GLProfile profile = GLProfile.getMaxProgrammableCore(true);
		GLCapabilities capabilities = new GLCapabilities(profile);
		myCanvas = new GLCanvas(capabilities);
		// end GL4 context
		myCanvas.addGLEventListener(this);
		getContentPane().add(myCanvas);
		setVisible(true);
		FPSAnimator animator = new FPSAnimator(myCanvas, 30);
		animator.start();
	}

	public void cameraControl() {
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();
				System.out.println(keyCode);
				if (keyCode == 39) {// --> pan right
					if (camera.getYRot() >= 36) {
						camera.setYRot(0f);
					} else {
						camera.setYRot(camera.getYRot() + (float) (0.0005f * 0.1));
					}
					center.setX(camera.getXPos() + (5 * Math.cos(17.5 + camera.getYRot())));
					center.setZ(camera.getZPos() + (5 * Math.sin(17.5 + camera.getYRot())));
					System.out.println("Y Rot amount: " + camera.getYRot() * 10 + " degrees");

				}
				if (keyCode == 37) {// <-- pan left
					if (camera.getYRot() >= 36 || camera.getYRot() <= -36) {
						camera.setYRot(0f);
					} else {
						camera.setYRot(camera.getYRot() - (float) (0.0005f * 0.1));
					}
					center.setX(camera.getXPos() - (5 * Math.cos(20.5 + camera.getYRot())));
					center.setZ(camera.getZPos() - (5 * Math.sin(20.5 + camera.getYRot())));
					System.out.println("Y Rot amount: " + camera.getYRot() * 10 + " degrees");
				}
				if (keyCode == 38) {// |^ pitch up
					if (camera.getXRot() >= 36) {
						camera.setXRot(0f);
					} else {
						camera.setXRot(camera.getXRot() + (float) (0.0005f * 0.1));
					}
					center.setZ(camera.getZPos() - (8 * Math.cos(0 + camera.getXRot())));
					center.setY(camera.getYPos() + (8 * Math.sin(0 + camera.getXRot())));
					System.out.println("X Rot amount: " + camera.getXRot() * 10 + " degrees");
				}
				if (keyCode == 40) {// |v pitch down
					if (camera.getXRot() >= 36) {
						camera.setYRot(0f);
					} else {
						camera.setXRot(camera.getXRot() - (float) (0.0005f * 0.1));
					}
					center.setZ(camera.getZPos() + (8 * Math.cos(3 + camera.getXRot())));
					center.setY(camera.getYPos() - (8 * Math.sin(3 + camera.getXRot())));
					System.out.println("X Rot amount: " + camera.getXRot() * 10 + " degrees");
				}
				if (keyCode == 81) {// q, move up
					camera.setYPos(camera.getYPos() + 0.0005f);
					center.setY(center.getY() + 0.0005f);
				}
				if (keyCode == 65) {// a, strafe left
					camera.setXPos(camera.getXPos() - 0.0005f);
					center.setX(center.getX() - 0.0005f);
				}
				if (keyCode == 69) {// e, move down
					camera.setYPos(camera.getYPos() - 0.0005f);
					center.setY(center.getY() - 0.0005f);
				}
				if (keyCode == 68) {// d, strafe right
					camera.setXPos(camera.getXPos() + 0.0005f);
					center.setX(center.getX() + 0.0005f);
				}
				if (keyCode == 87) {// w, move forward
					camera.setZPos(camera.getZPos() - 0.0005f);
					center.setZ(center.getZ() - 0.0005f);
				}
				if (keyCode == 83) {// s move backward
					camera.setZPos(camera.getZPos() + 0.0005f);
					center.setZ(center.getZ() + 0.0005f);
				}
				if (keyCode == 32) {// space, move light
					amt += 0.005f;
				}
				if (keyCode == 17) {// control, toggle light
					toggle = toggle * (-1);
				}
			}
		});
	}

	public void display(GLAutoDrawable drawable) {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		float depthClearVal[] = new float[1];
		depthClearVal[0] = 1.0f;
		gl.glClearBufferfv(GL_DEPTH, 0, depthClearVal, 0);

		currentLight.setPosition(lightLoc);
		//amt += 0.5f;
		l_matrix.setToIdentity();
		l_matrix.rotateY(amt);
		currentLight.setPosition(currentLight.getPosition().mult(l_matrix));
		if (toggle <0) {
			//currentLight.setPosition(sphLoc);
			currentLight.setPosition(outerSpace);

		} 

		

		// building the projection matrix
		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		proj_matrix = perspective(50.0f, aspect, 0.1f, 1000.0f);

		 float bkg[] = { 0.0f, 0.0f, 0.0f, 1.0f };
		 FloatBuffer bkgBuffer = Buffers.newDirectFloatBuffer(bkg);
		 gl.glClearBufferfv(GL_COLOR, 0, bkgBuffer);

		// draw cube map--------------------------------------

		gl.glUseProgram(rendering_program_cube_map);

		// put the V matrix into the corresponding uniforms
		cubeV_matrix = (Matrix3D) v_matrix.clone();
		cubeV_matrix.scale(1.0, -1.0, -1.0);
		int v_location = gl.glGetUniformLocation(rendering_program_cube_map, "v_matrix");
		gl.glUniformMatrix4fv(v_location, 1, false, cubeV_matrix.getFloatValues(), 0);

		// put the P matrix into the corresponding uniform
		int ploc = gl.glGetUniformLocation(rendering_program_cube_map, "p_matrix");
		gl.glUniformMatrix4fv(ploc, 1, false, proj_matrix.getFloatValues(), 0);
		// set up vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, textureID2);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		gl.glEnable(GL_DEPTH_TEST);

		gl.glBindFramebuffer(GL_FRAMEBUFFER, shadow_buffer[0]);
		gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadow_tex[0], 0);

		gl.glDrawBuffer(GL_NONE);
		gl.glEnable(GL_DEPTH_TEST);

		gl.glEnable(GL_POLYGON_OFFSET_FILL); // for reducing
		gl.glPolygonOffset(2.0f, 4.0f); // shadow artifacts

		passOne();

		gl.glDisable(GL_POLYGON_OFFSET_FILL); // artifact reduction, continued

		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, shadow_tex[0]);

		passTwo();

	}

//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public void passOne() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glUseProgram(rendering_program1);

		Point3D origin = new Point3D(0.0, 0.0, 0.0);
		Vector3D up = new Vector3D(0.0, 1.0, 0.0);
		lightV_matrix.setToIdentity();
		lightP_matrix.setToIdentity();

		lightV_matrix = lookAt(currentLight.getPosition(), origin, up); // vector from light to origin
		
		lightP_matrix = perspective(50.0f, aspect, 0.1f, 1000.0f);

		//draw light sphere

		
		m_matrix.setToIdentity();
		m_matrix.translate(currentLight.getPosition().getX(), currentLight.getPosition().getY(), currentLight.getPosition().getZ());
		m_matrix.scale(.01, .01, .01);

		shadowMVP1.setToIdentity();
		shadowMVP1.concatenate(lightP_matrix);
		shadowMVP1.concatenate(lightV_matrix);
		shadowMVP1.concatenate(m_matrix);


		// set up vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, textureID1); // normal

		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, textureID2); // texture

		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
	    gl.glDepthFunc(GL_LEQUAL);
	    
	    
		gl.glDrawArrays(GL_TRIANGLES, 0, mySphere.getIndices().length);
		
		
		
		// ----- draw sphere 1
		int shadow_location = gl.glGetUniformLocation(rendering_program1, "shadowMVP");

		m_matrix.setToIdentity();
		m_matrix.translate(sphLoc.getX(), sphLoc.getY(), sphLoc.getZ());

		shadowMVP1.setToIdentity();
		shadowMVP1.concatenate(lightP_matrix);
		shadowMVP1.concatenate(lightV_matrix);
		shadowMVP1.concatenate(m_matrix);

		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP1.getFloatValues(), 0);

		// set up vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, textureID1); // normal

		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, textureID2); // texture

		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, mySphere.getIndices().length);

		// ---- draw the pyramid

		// build the MODEL matrix
		m_matrix.setToIdentity();
		m_matrix.translate(pyrLoc.getX(), pyrLoc.getY(), pyrLoc.getZ());
		m_matrix.rotateX(30.0);
		m_matrix.rotateY(40.0);

		shadowMVP1.setToIdentity();
		shadowMVP1.concatenate(lightP_matrix);
		shadowMVP1.concatenate(lightV_matrix);
		shadowMVP1.concatenate(m_matrix);

		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP1.getFloatValues(), 0);

		// set up vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, pyramid.getNumVertices());

		// ----- draw the sphere
		m_matrix.setToIdentity();
		m_matrix.translate(torusLoc.getX(), torusLoc.getY(), torusLoc.getZ());

		shadowMVP1.setToIdentity();
		shadowMVP1.concatenate(lightP_matrix);
		shadowMVP1.concatenate(lightV_matrix);
		shadowMVP1.concatenate(m_matrix);

		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP1.getFloatValues(), 0);

		// set up vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, textureID1);

		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, mySphere.getIndices().length);

	}

//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public void passTwo() {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		gl.glUseProgram(rendering_program2);


		thisMaterial = graphicslib3D.Material.BRONZE;

		mv_location = gl.glGetUniformLocation(rendering_program2, "mv_matrix");
		proj_location = gl.glGetUniformLocation(rendering_program2, "proj_matrix");
		n_location = gl.glGetUniformLocation(rendering_program2, "normalMat");
		int shadow_location = gl.glGetUniformLocation(rendering_program2, "shadowMVP");
		// build the MODEL matrix
		m_matrix.setToIdentity();
		m_matrix.translate(torusLoc.getX(), torusLoc.getY(), torusLoc.getZ());
		m_matrix.rotateX(25.0);

		// build the VIEW matrix
		v_matrix.setToIdentity();
		// v_matrix.translate(-camera.getXPos(), -camera.getYPos(), -camera.getZPos());
		Vector3D y = new Vector3D(0, 1, 0);
		Point3D camPos = new Point3D(camera.getXPos(), camera.getYPos(), camera.getZPos());
		v_matrix.concatenate(lookAt(camPos, center, y));

		cameraControl();
		
		//light spehere
		
		installLights(rendering_program2, v_matrix);

		thisMaterial = graphicslib3D.Material.GOLD;
		
		// build the MODEL matrix
		m_matrix.setToIdentity();
		m_matrix.translate(currentLight.getPosition().getX(), currentLight.getPosition().getY(), currentLight.getPosition().getZ());
		m_matrix.scale(.01, .01, .01);

		// build the MODEL-VIEW matrix
		mv_matrix.setToIdentity();
		mv_matrix.concatenate(v_matrix);
		mv_matrix.concatenate(m_matrix);

		shadowMVP2.setToIdentity();
		shadowMVP2.concatenate(b);
		shadowMVP2.concatenate(lightP_matrix);
		shadowMVP2.concatenate(lightV_matrix);
		shadowMVP2.concatenate(m_matrix);
		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP2.getFloatValues(), 0);

		// put the MV and PROJ matrices into the corresponding uniforms
		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_location, 1, false, proj_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(), 0);

		// set up vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// set up the textures buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, earthTexture);

		// set up normals buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, mySphere.getIndices().length);
		
		
		
		
		thisMaterial = graphicslib3D.Material.BRONZE;
		// thisMaterial = graphicslib3D.Material.GOLD;
		installLights(rendering_program2, v_matrix);

		// build the MODEL matrix
		m_matrix.setToIdentity();
		m_matrix.translate(torusLoc.getX(), torusLoc.getY(), torusLoc.getZ());

		// build the MODEL-VIEW matrix
		mv_matrix.setToIdentity();
		mv_matrix.concatenate(v_matrix);
		mv_matrix.concatenate(m_matrix);

		shadowMVP2.setToIdentity();
		shadowMVP2.concatenate(b);
		shadowMVP2.concatenate(lightP_matrix);
		shadowMVP2.concatenate(lightV_matrix);
		shadowMVP2.concatenate(m_matrix);
		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP2.getFloatValues(), 0);

		// put the MV and PROJ matrices into the corresponding uniforms
		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_location, 1, false, proj_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(), 0);

		// set up vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// set up the textures buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, textureID1); // normal

		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, textureID2); // texture

		// set up normals buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		
		gl.glDrawArrays(GL_TRIANGLES, 0, mySphere.getIndices().length);
		//if (util.checkOpenGLError())
		//	System.out.println("=====Error after draw arrays pyramid");

		// pyramid---------------------------------------------------------------

		// thisMaterial = graphicslib3D.Material.GOLD;
		installLights(rendering_program2, v_matrix);

		// build the MODEL matrix
		m_matrix.setToIdentity();
		m_matrix.translate(pyrLoc.getX(), pyrLoc.getY(), pyrLoc.getZ());
		m_matrix.rotateX(30.0);
		m_matrix.rotateY(40.0);

		// build the MODEL-VIEW matrix
		mv_matrix.setToIdentity();
		mv_matrix.concatenate(v_matrix);
		mv_matrix.concatenate(m_matrix);

		shadowMVP2.setToIdentity();
		shadowMVP2.concatenate(b);
		shadowMVP2.concatenate(lightP_matrix);
		shadowMVP2.concatenate(lightV_matrix);
		shadowMVP2.concatenate(m_matrix);
		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP2.getFloatValues(), 0);

		// put the MV and PROJ matrices into the corresponding uniforms
		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_location, 1, false, proj_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(), 0);

		// set up vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// set up normals buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, pyramid.getNumVertices());
		//if (util.checkOpenGLError())
		//	System.out.println("=====Error after draw arrays pyramid");

		// sphere---------------------------------------------------------------
		// thisMaterial = graphicslib3D.Material.GOLD;
		installLights(rendering_program2, v_matrix);

		// build the MODEL matrix
		m_matrix.setToIdentity();
		m_matrix.translate(sphLoc.getX(), sphLoc.getY(), sphLoc.getZ());

		// build the MODEL-VIEW matrix
		mv_matrix.setToIdentity();
		mv_matrix.concatenate(v_matrix);
		mv_matrix.concatenate(m_matrix);

		shadowMVP2.setToIdentity();
		shadowMVP2.concatenate(b);
		shadowMVP2.concatenate(lightP_matrix);
		shadowMVP2.concatenate(lightV_matrix);
		shadowMVP2.concatenate(m_matrix);
		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP2.getFloatValues(), 0);

		// put the MV and PROJ matrices into the corresponding uniforms
		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_location, 1, false, proj_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(), 0);

		// set up vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// set up the textures buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, earthTexture);

		// set up normals buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, mySphere.getIndices().length);
		//if (util.checkOpenGLError())
			//System.out.println("=====Error after draw arrays pyramid");
	}

	public class Camera {
		private float cameraX, cameraY, cameraZ;
		private float rotX, rotY, rotZ;
		private float xAxis, yAxis, zAxis;
		private double radius;

		public void setupCamera(float Xp, float Yp, float Zp, float Xr, float Yr, float Zr) {
			cameraX = Xp;
			cameraY = Yp;
			cameraZ = Zp;
			rotX = Xr;
			rotY = Yr;
			rotZ = Zr;
		}

		public float getXPos() {
			return cameraX;
		}

		public void setXPos(float XPos) {
			cameraX = XPos;
		}

		public float getYPos() {
			return cameraY;
		}

		public void setYPos(float YPos) {
			cameraY = YPos;
		}

		public float getZPos() {
			return cameraZ;
		}

		public void setZPos(float ZPos) {
			cameraZ = ZPos;
		}

		public float getXRot() {
			return rotX;
		}

		public void setXRot(float XRot) {
			rotX = XRot;
		}

		public float getYRot() {
			return rotY;
		}

		public void setYRot(float YRot) {
			rotY = YRot;
		}

		public float setZRot() {
			return rotZ;
		}

		public void setZRot(float ZRot) {
			rotZ = ZRot;
		}

		public void setRadius(double r) {
			radius = r;
		}

		public double getRadius() {
			return radius;
		}
	}

	public void init(GLAutoDrawable drawable) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		createShaderPrograms();
		setupVertices();
		if (util.checkOpenGLError())
			System.out.println("Error after setupVertices");
		setupShadowBuffers();
		if (util.checkOpenGLError())
			System.out.println("Error after setupShadowBuffers");

		b.setElementAt(0, 0, 0.5);
		b.setElementAt(0, 1, 0.0);
		b.setElementAt(0, 2, 0.0);
		b.setElementAt(0, 3, 0.5f);
		b.setElementAt(1, 0, 0.0);
		b.setElementAt(1, 1, 0.5);
		b.setElementAt(1, 2, 0.0);
		b.setElementAt(1, 3, 0.5f);
		b.setElementAt(2, 0, 0.0);
		b.setElementAt(2, 1, 0.0);
		b.setElementAt(2, 2, 0.5);
		b.setElementAt(2, 3, 0.5f);
		b.setElementAt(3, 0, 0.0);
		b.setElementAt(3, 1, 0.0);
		b.setElementAt(3, 2, 0.0);
		b.setElementAt(3, 3, 1.0f);

		// may reduce shadow border artifacts
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

		textureID2 = loadCubeMap();
		gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);

		Texture t = loadTexture("ModelsTextures/PlanetPixelEmporium/earthmap1k.jpg");
		textureID1 = t.getTextureObject();

		joglEarthTexture = loadTexture("ModelsTextures/PlanetPixelEmporium/earthmap1k.jpg");
		earthTexture = joglEarthTexture.getTextureObject();

		joglMarsTexture = loadTexture("ModelsTextures/PlanetPixelEmporium/mars.jpg");
		marsTexture = joglMarsTexture.getTextureObject();

		camera.setupCamera(0f, 0f, 6f, 0f, 0f, 0f);
		
		earthTex = loadTexture("ProgData/Prog10_3_data/earthTexture.jpg");  // earth surface texture
//		moonTex = loadTexture("ProgData/Prog10_3_data/moon.jpg");  // moon surface texture
		textureID2 = earthTex.getTextureObject();
		
		// apply mipmapping and anisotropic filtering to moon surface texture
		gl.glBindTexture(GL_TEXTURE_2D, textureID2);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		gl.glGenerateMipmap(GL_TEXTURE_2D);
		if (gl.isExtensionAvailable("GL_EXT_texture_filter_anisotropic"))
		{	float aniso[] = new float[1];
			gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, aniso, 0);
			gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, aniso[0]);
		}
		

	}

	public void setupShadowBuffers() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		scSizeX = myCanvas.getWidth();
		scSizeY = myCanvas.getHeight();

		gl.glGenFramebuffers(1, shadow_buffer, 0);
		if (util.checkOpenGLError())
			System.out.println("============Error after genFrameBuffers");

		gl.glGenTextures(1, shadow_tex, 0);
		gl.glBindTexture(GL_TEXTURE_2D, shadow_tex[0]);
		if (util.checkOpenGLError())
			System.out.println("============Error after BindTexture");
		if (util.checkOpenGLError())
			System.out.println("============Error after reading uniform location");
		if (util.checkOpenGLError())
			System.out.println("============Error after setting uniform");
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32, scSizeX, scSizeY, 0, GL_DEPTH_COMPONENT, GL_FLOAT,
				null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
	}

// -----------------------------
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		setupShadowBuffers();
	}

	public float[][] PTNvalues() {
		Vertex3D[] vertices = mySphere.getVertices();
		int[] indices = mySphere.getIndices();

		float[] pvalues = new float[indices.length * 3];
		float[] tvalues = new float[indices.length * 2];
		float[] nvalues = new float[indices.length * 3];
		float[] TANvals = new float[indices.length*3];


		float[][] ptnValues = new float[4][1];

		for (int i = 0; i < indices.length; i++) {
			pvalues[i * 3] = (float) (vertices[indices[i]]).getX();
			pvalues[i * 3 + 1] = (float) (vertices[indices[i]]).getY();
			pvalues[i * 3 + 2] = (float) (vertices[indices[i]]).getZ();
			tvalues[i * 2] = (float) (vertices[indices[i]]).getS();
			tvalues[i * 2 + 1] = (float) (vertices[indices[i]]).getT();
			nvalues[i * 3] = (float) (vertices[indices[i]]).getNormalX();
			nvalues[i * 3 + 1] = (float) (vertices[indices[i]]).getNormalY();
			nvalues[i * 3 + 2] = (float) (vertices[indices[i]]).getNormalZ();
			
			TANvals[i*3] = (float) (vertices[indices[i]]).getTangent().getX();
			TANvals[i*3+1] = (float) (vertices[indices[i]]).getTangent().getY();
			TANvals[i*3+2] = (float) (vertices[indices[i]]).getTangent().getZ();
		}

		ptnValues[0] = pvalues;
		ptnValues[1] = tvalues;
		ptnValues[2] = nvalues;
		ptnValues[3] = TANvals;

		return ptnValues;

	}

	private void setupVertices() {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		// pyramid definition
		Vertex3D[] pyramid_vertices = pyramid.getVertices();
		numPyramidVertices = pyramid.getNumVertices();

		float[] pyramid_vertex_positions = new float[numPyramidVertices * 3];
		float[] pyramid_normals = new float[numPyramidVertices * 3];

		for (int i = 0; i < numPyramidVertices; i++) {
			pyramid_vertex_positions[i * 3] = (float) (pyramid_vertices[i]).getX();
			pyramid_vertex_positions[i * 3 + 1] = (float) (pyramid_vertices[i]).getY();
			pyramid_vertex_positions[i * 3 + 2] = (float) (pyramid_vertices[i]).getZ();

			pyramid_normals[i * 3] = (float) (pyramid_vertices[i]).getNormalX();
			pyramid_normals[i * 3 + 1] = (float) (pyramid_vertices[i]).getNormalY();
			pyramid_normals[i * 3 + 2] = (float) (pyramid_vertices[i]).getNormalZ();
		}

		Vertex3D[] torus_vertices = myTorus.getVertices();

		int[] torus_indices = myTorus.getIndices();
		float[] torus_fvalues = new float[torus_indices.length * 3];
		float[] torus_nvalues = new float[torus_indices.length * 3];

		for (int i = 0; i < torus_indices.length; i++) {
			torus_fvalues[i * 3] = (float) (torus_vertices[torus_indices[i]]).getX();
			torus_fvalues[i * 3 + 1] = (float) (torus_vertices[torus_indices[i]]).getY();
			torus_fvalues[i * 3 + 2] = (float) (torus_vertices[torus_indices[i]]).getZ();

			torus_nvalues[i * 3] = (float) (torus_vertices[torus_indices[i]]).getNormalX();
			torus_nvalues[i * 3 + 1] = (float) (torus_vertices[torus_indices[i]]).getNormalY();
			torus_nvalues[i * 3 + 2] = (float) (torus_vertices[torus_indices[i]]).getNormalZ();
		}

		numTorusVertices = torus_indices.length;

		float[][] ptnValues = PTNvalues();
		float[] pvalues = ptnValues[0];
		float[] tvalues = ptnValues[1];
		float[] nvalues = ptnValues[2];
		float[] TANvals = ptnValues[3];

		float[] cube_vertices = { -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f,
				1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f,
				1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
				-1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f,
				1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f,
				1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f,
				-1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f };

		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);

		gl.glGenBuffers(9, vbo, 0);

		// put the Torus vertices into the first buffer,
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(torus_fvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);

		// load the pyramid vertices into the second buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer pyrVertBuf = Buffers.newDirectFloatBuffer(pyramid_vertex_positions);
		gl.glBufferData(GL_ARRAY_BUFFER, pyrVertBuf.limit() * 4, pyrVertBuf, GL_STATIC_DRAW);

		// load the torus normal coordinates into the third buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer torusNorBuf = Buffers.newDirectFloatBuffer(torus_nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, torusNorBuf.limit() * 4, torusNorBuf, GL_STATIC_DRAW);

		// load the pyramid normal coordinates into the fourth buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer pyrNorBuf = Buffers.newDirectFloatBuffer(pyramid_normals);
		gl.glBufferData(GL_ARRAY_BUFFER, pyrNorBuf.limit() * 4, pyrNorBuf, GL_STATIC_DRAW);

		//sphere
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer vBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vBuf.limit() * 4, vBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		FloatBuffer tBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, tBuf.limit() * 4, tBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		FloatBuffer nBuf = Buffers.newDirectFloatBuffer(nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, nBuf.limit() * 4, nBuf, GL_STATIC_DRAW);

		//skybox
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		FloatBuffer cubeVertBuf = Buffers.newDirectFloatBuffer(cube_vertices);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeVertBuf.limit() * 4, cubeVertBuf, GL_STATIC_DRAW);
	
		//sphere tan
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		FloatBuffer tanBuf = Buffers.newDirectFloatBuffer(TANvals);
		gl.glBufferData(GL_ARRAY_BUFFER, tanBuf.limit()*4, tanBuf, GL_STATIC_DRAW);
	}

	private void installLights(int rendering_program, Matrix3D v_matrix) {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		Material currentMaterial = new Material();
		currentMaterial = thisMaterial;

		Point3D lightP = currentLight.getPosition();
		Point3D lightPv = lightP.mult(v_matrix);

		float[] currLightPos = new float[] { (float) lightPv.getX(), (float) lightPv.getY(), (float) lightPv.getZ() };

		// get the location of the global ambient light field in the shader
		int globalAmbLoc = gl.glGetUniformLocation(rendering_program, "globalAmbient");

		// set the current globalAmbient settings
		gl.glProgramUniform4fv(rendering_program, globalAmbLoc, 1, globalAmbient, 0);

		// get the locations of the light and material fields in the shader
		int ambLoc = gl.glGetUniformLocation(rendering_program, "light.ambient");
		int diffLoc = gl.glGetUniformLocation(rendering_program, "light.diffuse");
		int specLoc = gl.glGetUniformLocation(rendering_program, "light.specular");
		int posLoc = gl.glGetUniformLocation(rendering_program, "light.position");

		int MambLoc = gl.glGetUniformLocation(rendering_program, "material.ambient");
		int MdiffLoc = gl.glGetUniformLocation(rendering_program, "material.diffuse");
		int MspecLoc = gl.glGetUniformLocation(rendering_program, "material.specular");
		int MshiLoc = gl.glGetUniformLocation(rendering_program, "material.shininess");

		// set the uniform light and material values in the shader
		gl.glProgramUniform4fv(rendering_program, ambLoc, 1, currentLight.getAmbient(), 0);
		gl.glProgramUniform4fv(rendering_program, diffLoc, 1, currentLight.getDiffuse(), 0);
		gl.glProgramUniform4fv(rendering_program, specLoc, 1, currentLight.getSpecular(), 0);
		gl.glProgramUniform3fv(rendering_program, posLoc, 1, currLightPos, 0);

		gl.glProgramUniform4fv(rendering_program, MambLoc, 1, currentMaterial.getAmbient(), 0);
		gl.glProgramUniform4fv(rendering_program, MdiffLoc, 1, currentMaterial.getDiffuse(), 0);
		gl.glProgramUniform4fv(rendering_program, MspecLoc, 1, currentMaterial.getSpecular(), 0);
		gl.glProgramUniform1f(rendering_program, MshiLoc, currentMaterial.getShininess());
	}

	public static void main(String[] args) {
		new Prog08_1_shadowMapping();
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		GL4 gl = (GL4) drawable.getGL();
		gl.glDeleteVertexArrays(1, vao, 0);
	}

//-----------------
	private void createShaderPrograms() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int[] vertCompiled = new int[1];
		int[] fragCompiled = new int[1];
		int[] linked = new int[1];
		System.out.println(GLContext.getCurrent().getGLVersion());
		System.out.println(GLContext.getCurrent().getGLSLVersionString());

		vBlinn1ShaderSource = util.readShaderSource("ProgData/Prog8_1_data/blinnVert1.shader");
		fBlinn1ShaderSource = util.readShaderSource("ProgData/Prog8_1_data/blinnFrag1.shader");
		vBlinn2ShaderSource = util.readShaderSource("ProgData/Prog8_1_data/blinnVert2.shader");
		fBlinn2ShaderSource = util.readShaderSource("ProgData/Prog8_1_data/blinnFrag2.shader");
		

		int vertexShader1 = gl.glCreateShader(GL_VERTEX_SHADER);
		int fragmentShader1 = gl.glCreateShader(GL_FRAGMENT_SHADER);
		int vertexShader2 = gl.glCreateShader(GL_VERTEX_SHADER);
		int fragmentShader2 = gl.glCreateShader(GL_FRAGMENT_SHADER);

		gl.glShaderSource(vertexShader1, vBlinn1ShaderSource.length, vBlinn1ShaderSource, null, 0);
		gl.glShaderSource(fragmentShader1, fBlinn1ShaderSource.length, fBlinn1ShaderSource, null, 0);
		gl.glShaderSource(vertexShader2, vBlinn2ShaderSource.length, vBlinn2ShaderSource, null, 0);
		gl.glShaderSource(fragmentShader2, fBlinn2ShaderSource.length, fBlinn2ShaderSource, null, 0);

		gl.glCompileShader(vertexShader1);
		util.checkOpenGLError(); // can use returned boolean
		gl.glGetShaderiv(vertexShader1, GL_COMPILE_STATUS, vertCompiled, 0);
		if (vertCompiled[0] == 1) {
			System.out.println("vertex compilation success");
		} else {
			System.out.println("vertex compilation failed");
			util.printShaderLog(vertexShader1);
		}
		gl.glCompileShader(fragmentShader1);
		util.checkOpenGLError(); // can use returned boolean
		gl.glGetShaderiv(fragmentShader1, GL_COMPILE_STATUS, fragCompiled, 0);
		if (fragCompiled[0] == 1) {
			System.out.println("fragment compilation success");
		} else {
			System.out.println("fragment compilation failed");
			util.printShaderLog(fragmentShader1);
		}
		gl.glCompileShader(vertexShader2);
		util.checkOpenGLError(); // can use returned boolean
		gl.glGetShaderiv(vertexShader2, GL_COMPILE_STATUS, vertCompiled, 0);
		if (vertCompiled[0] == 1) {
			System.out.println("vertex compilation success");
		} else {
			System.out.println("vertex compilation failed");
			util.printShaderLog(vertexShader2);
		}

		gl.glCompileShader(fragmentShader2);
		util.checkOpenGLError(); // can use returned boolean
		gl.glGetShaderiv(fragmentShader2, GL_COMPILE_STATUS, fragCompiled, 0);
		if (fragCompiled[0] == 1) {
			System.out.println("fragment compilation success");
		} else {
			System.out.println("fragment compilation failed");
			util.printShaderLog(fragmentShader2);
		}

		rendering_program1 = gl.glCreateProgram();
		rendering_program2 = gl.glCreateProgram();

		gl.glAttachShader(rendering_program1, vertexShader1);
		gl.glAttachShader(rendering_program1, fragmentShader1);
		gl.glAttachShader(rendering_program2, vertexShader2);
		gl.glAttachShader(rendering_program2, fragmentShader2);

		gl.glLinkProgram(rendering_program1);
		util.checkOpenGLError();
		gl.glGetProgramiv(rendering_program1, GL_LINK_STATUS, linked, 0);
		if (linked[0] == 1) {
			System.out.println("linking succeeded");
		} else {
			System.out.println("linking failed");
			util.printProgramLog(rendering_program1);
		}

		gl.glLinkProgram(rendering_program2);
		util.checkOpenGLError();
		gl.glGetProgramiv(rendering_program2, GL_LINK_STATUS, linked, 0);
		if (linked[0] == 1) {
			System.out.println("linking succeeded");
		} else {
			System.out.println("linking failed");
			util.printProgramLog(rendering_program1);
		}

		// rendering program for cube map
		vertShaderSource = util.readShaderSource("ProgData/Prog9_2_data/vertC.shader");
		fragShaderSource = util.readShaderSource("ProgData/Prog9_2_data/fragC.shader");

		int vertexShader = gl.glCreateShader(GL_VERTEX_SHADER);
		int fragmentShader = gl.glCreateShader(GL_FRAGMENT_SHADER);

		gl.glShaderSource(vertexShader, vertShaderSource.length, vertShaderSource, null, 0);
		gl.glShaderSource(fragmentShader, fragShaderSource.length, fragShaderSource, null, 0);

		gl.glCompileShader(vertexShader);
		util.checkOpenGLError(); // can use returned boolean
		gl.glGetShaderiv(vertexShader, GL_COMPILE_STATUS, vertCompiled, 0);
		if (vertCompiled[0] == 1) {
			System.out.println("vertex compilation success");
		} else {
			System.out.println("vertex compilation failed");
			util.printShaderLog(vertexShader);
		}
		gl.glCompileShader(fragmentShader);
		util.checkOpenGLError(); // can use returned boolean
		gl.glGetShaderiv(fragmentShader, GL_COMPILE_STATUS, fragCompiled, 0);
		if (fragCompiled[0] == 1) {
			System.out.println("fragment compilation success");
		} else {
			System.out.println("fragment compilation failed");
			util.printShaderLog(fragmentShader);
		}

		rendering_program_cube_map = gl.glCreateProgram();
		gl.glAttachShader(rendering_program_cube_map, vertexShader);
		gl.glAttachShader(rendering_program_cube_map, fragmentShader);
		gl.glLinkProgram(rendering_program_cube_map);
		util.checkOpenGLError();
		gl.glGetProgramiv(rendering_program_cube_map, GL_LINK_STATUS, linked, 0);
		if (linked[0] == 1) {
			System.out.println("linking succeeded");
		} else {
			System.out.println("linking failed");
			util.printProgramLog(rendering_program_cube_map);
		}

	}

//------------------
	private Matrix3D perspective(float fovy, float aspect, float n, float f) {
		float q = 1.0f / ((float) Math.tan(Math.toRadians(0.5f * fovy)));
		float A = q / aspect;
		float B = (n + f) / (n - f);
		float C = (2.0f * n * f) / (n - f);
		Matrix3D r = new Matrix3D();
		r.setElementAt(0, 0, A);
		r.setElementAt(1, 1, q);
		r.setElementAt(2, 2, B);
		r.setElementAt(3, 2, -1.0f);
		r.setElementAt(2, 3, C);
		r.setElementAt(3, 3, 0.0f);
		return r;
	}

	private Matrix3D lookAt(Point3D eye, Point3D target, Vector3D y) {
		Vector3D eyeV = new Vector3D(eye);
		Vector3D targetV = new Vector3D(target);
		Vector3D fwd = (targetV.minus(eyeV)).normalize();
		Vector3D side = (fwd.cross(y)).normalize();
		Vector3D up = (side.cross(fwd)).normalize();
		Matrix3D look = new Matrix3D();
		look.setElementAt(0, 0, side.getX());
		look.setElementAt(1, 0, up.getX());
		look.setElementAt(2, 0, -fwd.getX());
		look.setElementAt(3, 0, 0.0f);
		look.setElementAt(0, 1, side.getY());
		look.setElementAt(1, 1, up.getY());
		look.setElementAt(2, 1, -fwd.getY());
		look.setElementAt(3, 1, 0.0f);
		look.setElementAt(0, 2, side.getZ());
		look.setElementAt(1, 2, up.getZ());
		look.setElementAt(2, 2, -fwd.getZ());
		look.setElementAt(3, 2, 0.0f);
		look.setElementAt(0, 3, side.dot(eyeV.mult(-1)));
		look.setElementAt(1, 3, up.dot(eyeV.mult(-1)));
		look.setElementAt(2, 3, (fwd.mult(-1)).dot(eyeV.mult(-1)));
		look.setElementAt(3, 3, 1.0f);
		return (look);
	}

	private int loadCubeMap() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		GLProfile glp = gl.getGLProfile();
		Texture tex = new Texture(GL_TEXTURE_CUBE_MAP);

		try {
			TextureData topFile = TextureIO.newTextureData(glp, new File("ProgData/Prog9_2_data/top.jpg"), false,
					"jpg");
			TextureData leftFile = TextureIO.newTextureData(glp, new File("ProgData/Prog9_2_data/left.jpg"), false,
					"jpg");
			TextureData fntFile = TextureIO.newTextureData(glp, new File("ProgData/Prog9_2_data/center.jpg"), false,
					"jpg");
			TextureData rightFile = TextureIO.newTextureData(glp, new File("ProgData/Prog9_2_data/right.jpg"), false,
					"jpg");
			TextureData bkFile = TextureIO.newTextureData(glp, new File("ProgData/Prog9_2_data/back.jpg"), false,
					"jpg");
			TextureData botFile = TextureIO.newTextureData(glp, new File("ProgData/Prog9_2_data/bottom.jpg"), false,
					"jpg");

			tex.updateImage(gl, rightFile, GL_TEXTURE_CUBE_MAP_POSITIVE_X);
			tex.updateImage(gl, leftFile, GL_TEXTURE_CUBE_MAP_NEGATIVE_X);
			tex.updateImage(gl, botFile, GL_TEXTURE_CUBE_MAP_POSITIVE_Y);
			tex.updateImage(gl, topFile, GL_TEXTURE_CUBE_MAP_NEGATIVE_Y);
			tex.updateImage(gl, fntFile, GL_TEXTURE_CUBE_MAP_POSITIVE_Z);
			tex.updateImage(gl, bkFile, GL_TEXTURE_CUBE_MAP_NEGATIVE_Z);
		} catch (IOException | GLException e) {
		}

		int[] textureIDs = new int[1];
		gl.glGenTextures(1, textureIDs, 0);
		int textureID = tex.getTextureObject();

		// reduce seams
		gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);

		return textureID;
	}

	public Texture loadTexture(String textureFileName) {
		Texture tex = null;
		try {
			tex = TextureIO.newTexture(new File(textureFileName), false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tex;
	}
}