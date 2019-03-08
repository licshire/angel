/*
 * Tencent is pleased to support the open source community by making Angel available.
 *
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/Apache-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.tencent.angel.spark.ml.psf.embedding.w2v;

import com.tencent.angel.PartitionKey;
import com.tencent.angel.ml.math2.storage.IntFloatDenseVectorStorage;
import com.tencent.angel.ml.matrix.psf.update.base.PartitionUpdateParam;
import com.tencent.angel.ml.matrix.psf.update.base.UpdateFunc;
import com.tencent.angel.spark.ml.psf.embedding.ServerWrapper;

public class Adjust extends UpdateFunc {

  public Adjust(AdjustParam param) {
    super(param);
  }

  public Adjust() { super(null); }

  public void partitionUpdate(PartitionUpdateParam partParam) {

    if (partParam instanceof AdjustPartitionParam) {

      AdjustPartitionParam param = (AdjustPartitionParam) partParam;

      try {
        // Some params
        PartitionKey pkey = param.getPartKey();

        int seed = param.seed;
        int order = 2;

        int[][] sentences = param.sentences;
        int maxIndex = ServerWrapper.getMaxIndex();
        int maxLength = ServerWrapper.getMaxLength();
        int negative = ServerWrapper.getNegative();
        int partDim = ServerWrapper.getPartDim();
        int window = ServerWrapper.getWindow();

        // compute number of nodes for one row
        int size = (int) (pkey.getEndCol() - pkey.getStartCol());
        int numNodes = size / (partDim * order);

        int numRows = pkey.getEndRow() - pkey.getStartRow();
        float[][] layers = new float[numRows][];
        for (int row = 0; row < numRows; row ++)
          layers[row] = ((IntFloatDenseVectorStorage) psContext.getMatrixStorageManager()
                  .getRow(pkey, row).getSplit().getStorage())
                  .getValues();

        EmbeddingModel model;
        switch (param.model) {
          case 0:
            model = new SkipgramModel(partDim, negative, window, seed, maxIndex, numNodes, maxLength, layers);
            break;
          default:
            model = new CbowModel(partDim, negative, window, seed, maxIndex, numNodes, maxLength, layers);
        }


        int numInputs = ServerWrapper.getNumInputs(param.partitionId);
        int numOutputs = ServerWrapper.getNumOutputs(param.partitionId);

        model.adjust(sentences, param.buf, numInputs, numOutputs);
      } finally {
        param.clear();
      }
    }

  }

}
