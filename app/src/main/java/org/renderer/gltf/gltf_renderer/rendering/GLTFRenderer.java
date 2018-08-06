/**
 * Copyright 2018 Facebook Inc. All Rights Reserved.
 *
 * Licensed under the Creative Commons CC BY-NC 4.0 Attribution-NonCommercial
 * License (the "License"). You may obtain a copy of the License at
 * https://creativecommons.org/licenses/by-nc/4.0/.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.renderer.gltf.gltf_renderer.rendering;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import org.renderer.gltf.gltf_renderer.gles.GLHelpers;
import org.renderer.gltf.gltf_renderer.gles.ShaderProgram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import de.javagl.jgltf.model.AccessorDatas;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.BufferModel;
import de.javagl.jgltf.model.BufferViewModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.io.GltfModelReader;

/**
 * This is a trivial glTF renderer that issues GLES draw commands to render the primitive meshes
 * parsed by <em>GLTFReader</em>. Note that this renderer is a sample that is only intended to
 * render helloworld.gltf. This is not intended to be a generic glTF renderer; it is intended to
 * be an introduction to 3D scene rendering with OpenGL ES APIs and the glTF format.
 */
public class GLTFRenderer {
    private static final String TAG = GLTFRenderer.class.getSimpleName();

    private static final int ELEMENTS_PER_POSITION = 3;
    private static final int ELEMENTS_PER_NORMAL = 3;
    private static final int ELEMENTS_PER_TANGENT = 4;
    private static final int ELEMENTS_PER_TEXCOORD = 2;
    private static final int ELEMENTS_PER_COLOR = 4;
    //private static final int ELEMENTS_PER_JOINTS = 4;
    //private static final int ELEMENTS_PER_WEIGHTS = 4;

    private static final int BYTES_PER_FLOAT = 4;
    private static final int BYTES_PER_SHORT = 2;

    // The currently loaded model.
    private GltfModel gltfModel;

    // Actual GL resources.
    private int [] bufferModels = null;
    private int [] textureModels = null;

    // Mapping models to theirs respective GL types.
    private Map<BufferModel, Integer> bufferMap = new HashMap<>();
    private Map<TextureModel, Integer> textureMap = new HashMap<>();

    // The currently active shader program.
    private ShaderProgram shaderProgram;

    // Shader locations;
    private int modelViewProjectionUniform;
    private int positionAttributeLocation;
    private int normalAttributeLocation;
    private int tangentAttributeLocation;
    private int texcoord_0AttributeLocation;
    private int texcoord_1AttributeLocation;
    private int texcoord_2AttributeLocation;
    private int texcoord_3AttributeLocation;
    private int color_0AttributeLocation;
    //private int joints_0AttributeLocation;
    //private int weights_0AttributeLocation;

    // Transformation matrices.
    private final float[] modelMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];

    public GLTFRenderer() {}

    // Clears all allocated GPU data.
    private void clearResources() {
        if(bufferModels != null) {
            GLES20.glDeleteBuffers(bufferModels.length, bufferModels, 0);
            bufferModels = null;
        }
        bufferMap.clear();

        if(textureModels != null) {
            GLES20.glDeleteTextures(textureModels.length, textureModels, 0);
            textureModels = null;
        }
        textureMap.clear();
    }


    // Prepares render data for each glTF mesh primitive.
    private void createResources() {

        if(gltfModel == null) {
            return;
        }

        // Load buffer data.
        bufferModels = new int[gltfModel.getBufferModels().size()];
        GLES20.glGenBuffers(bufferModels.length, bufferModels, 0);
        for(int i=0; i<bufferModels.length; i++) {
            BufferModel bufferModel = gltfModel.getBufferModels().get(i);
            bufferMap.put(bufferModel, i);
//            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferModels[i]);
//            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, bufferModel.getByteLength(), bufferModel.getBufferData(), GLES20.GL_STATIC_DRAW);
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLHelpers.checkGlError("Buffer data");

        // Load texture data.
        textureModels = new int[gltfModel.getTextureModels().size()];
        GLES20.glGenTextures(textureModels.length, textureModels, 0);
        for(int i=0; i<textureModels.length; i++) {
            TextureModel textureModel = gltfModel.getTextureModels().get(i);
            textureMap.put(textureModel, i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureModels[i]);
            if(textureModel.getImageModel().getImageData() != null) {
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1, 1, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, textureModel.getImageModel().getImageData());
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, textureModel.getMinFilter());
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, textureModel.getMagFilter());
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, textureModel.getWrapS());
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, textureModel.getWrapT());
            } else {
                Log.e(TAG, "Image data null");
            }
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLHelpers.checkGlError("Texture data");
    }

    private void setupAttribute(MeshPrimitiveModel meshPrimitive, String attribute, int index) {

        // Check if this attribute is currently handles by our shader.
        if(index < 0) {
            return;
        }

        AccessorModel accessor = meshPrimitive.getAttributes().get(attribute);
        if(accessor != null) {

            BufferViewModel bufferView = accessor.getBufferViewModel();
            BufferModel buffer = bufferView.getBufferModel();
            Integer bufferId = bufferMap.get(buffer);

            int offset = accessor.getByteOffset() + bufferView.getByteOffset();
            int stride = accessor.getByteStride();
            if(bufferView.getByteStride() != null) {
                stride += bufferView.getByteStride();
            }

            GLES20.glBindBuffer(bufferView.getTarget(), bufferId);
            GLES20.glVertexAttribPointer(index, ELEMENTS_PER_POSITION, GLES20.GL_FLOAT, false, stride, offset);
            GLES20.glEnableVertexAttribArray(index);
            GLES20.glBindBuffer(bufferView.getTarget(), 0);

        } else {
            GLES20.glDisableVertexAttribArray(index);
        }
    }

    private static String readAsset(Context context, String asset) {
        try {
            InputStream is = context.getAssets().open(asset);
            InputStreamReader reader = new InputStreamReader(is);
            BufferedReader buf = new BufferedReader(reader);
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = buf.readLine()) != null) {
                text.append(line).append('\n');
            }
            return text.toString();
        } catch (IOException e) {
            return null;
        }
    }

    public void createOnGlThread(Context context, String glTFAssetName)
            throws IOException {

        clearResources();

        shaderProgram = new ShaderProgram(
                readAsset(context, "gltfobjectvert.glsl"),
                readAsset(context, "gltfobjectfrag.glsl"));

        GLES20.glUseProgram(shaderProgram.getShaderHandle());

        modelViewProjectionUniform  = shaderProgram.getUniform("u_ModelViewProjection");
        positionAttributeLocation   = shaderProgram.getAttribute("a_Position");
        normalAttributeLocation     = shaderProgram.getAttribute("a_Normal");
        tangentAttributeLocation    = shaderProgram.getAttribute("a_Tangent");
        texcoord_0AttributeLocation = shaderProgram.getAttribute("a_Texcoord_0");
        texcoord_1AttributeLocation = shaderProgram.getAttribute("a_Texcoord_1");
        texcoord_2AttributeLocation = shaderProgram.getAttribute("a_Texcoord_2");
        texcoord_3AttributeLocation = shaderProgram.getAttribute("a_Texcoord_3");
        color_0AttributeLocation    = shaderProgram.getAttribute("a_Color_0");
        //joints_0AttributeLocation   = shaderProgram.getAttribute("a_Joints_0");
        //weights_0AttributeLocation  = shaderProgram.getAttribute("a_Weights_0");

        Matrix.setIdentityM(modelMatrix, 0);

        try {
            // Create reader.
            GltfModelReader r = new GltfModelReader();

            // Read model.
            InputStream stream = context.getAssets().open(glTFAssetName);
            gltfModel = r.readWithoutReferences(stream);
            stream.close();

            // Create GL resources.
            createResources();

            Log.i(TAG, "Loading was successful");
        } catch(Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    public void updateModelMatrix(float[] modelMatrix, float scaleFactor) {
        float[] scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);
        scaleMatrix[0] = scaleFactor;
        scaleMatrix[5] = scaleFactor;
        scaleMatrix[10] = scaleFactor;
        Matrix.multiplyMM(this.modelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
    }

    public void draw(float[] cameraView, float[] cameraPerspective) {
        GLHelpers.checkGlError("Before draw");

        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);

        GLES20.glUseProgram(shaderProgram.getShaderHandle());

        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);

        for(SceneModel scene : gltfModel.getSceneModels()) {
            for(NodeModel node : scene.getNodeModels()) {
                for(MeshModel mesh : node.getMeshModels()) {
                    for(MeshPrimitiveModel meshPrimitive : mesh.getMeshPrimitiveModels()) {

                        // Setup attribute pointers.
                        setupAttribute(meshPrimitive, "POSITION", positionAttributeLocation);
                        setupAttribute(meshPrimitive, "NORMAL", normalAttributeLocation);
                        setupAttribute(meshPrimitive, "TANGENT", tangentAttributeLocation);
                        setupAttribute(meshPrimitive, "TEXCOORD_0", texcoord_0AttributeLocation);
                        setupAttribute(meshPrimitive, "TEXCOORD_1", texcoord_1AttributeLocation);
                        setupAttribute(meshPrimitive, "TEXCOORD_2", texcoord_2AttributeLocation);
                        setupAttribute(meshPrimitive, "TEXCOORD_3", texcoord_3AttributeLocation);
                        setupAttribute(meshPrimitive, "COLOR_0", color_0AttributeLocation);

                        if(meshPrimitive.getIndices() != null) {
                            //GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, );
                            //int numElements = meshPrimitive.getIndices().getBufferViewModel().getByteLength() / BYTES_PER_SHORT;
                            //GLES20.glDrawElements(meshPrimitive.getMode(), );
                            //GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
                        } else {
                            //GLES20.glDrawArrays(meshPrimitive.getMode(), meshPrimitive.);
                        }

                    }
                }
            }
        }

        GLHelpers.checkGlError("After draw");
    }

    public void release() {
       shaderProgram.release();
       clearResources();
    }
}
