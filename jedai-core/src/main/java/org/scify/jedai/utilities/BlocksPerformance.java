/*
* Copyright [2016-2018] [George Papadakis (gpapadis@yahoo.gr)]
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
 */
package org.scify.jedai.utilities;

import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.GroundTruthIndex;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.BilateralBlock;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.ComparisonIterator;
import org.scify.jedai.datamodel.DecomposedBlock;
import org.scify.jedai.datamodel.IdDuplicates;
import org.scify.jedai.datamodel.UnilateralBlock;

import com.esotericsoftware.minlog.Log;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import java.util.List;
import org.scify.jedai.blockprocessing.comparisoncleaning.ComparisonPropagation;
import org.scify.jedai.datamodel.EntityProfile;

/**
 *
 * @author gap2
 */
public class BlocksPerformance {

    private boolean isCleanCleanER;

    private int noOfD1Entities;
    private int noOfD2Entities;
    private int detectedDuplicates;

    private double aggregateCardinality;
    private double blockAssignments;
    private double d1BlockAssignments;
    private double d2BlockAssignments;
    private double fMeasure;
    private double pc;
    private double pq;

    private final AbstractDuplicatePropagation abstractDP;
    private final List<AbstractBlock> blocks;
    private GroundTruthIndex entityIndex;

    public BlocksPerformance(List<AbstractBlock> bl, AbstractDuplicatePropagation adp) {
        abstractDP = adp;
        abstractDP.resetDuplicates();
        blocks = bl;
    }

    private boolean areCooccurring(boolean cleanCleanER, IdDuplicates pairOfDuplicates) {
        final int[] blocks1 = entityIndex.getEntityBlocks(pairOfDuplicates.getEntityId1(), 0);
        if (blocks1 == null) {
            return false;
        }

        final int[] blocks2 = entityIndex.getEntityBlocks(pairOfDuplicates.getEntityId2(), cleanCleanER ? 1 : 0);
        if (blocks2 == null) {
            return false;
        }

        int noOfBlocks1 = blocks1.length;
        int noOfBlocks2 = blocks2.length;
        for (int i = 0; i < noOfBlocks1; i++) {
            for (int j = 0; j < noOfBlocks2; j++) {
                if (blocks2[j] < blocks1[i]) {
                    continue;
                }

                if (blocks1[i] < blocks2[j]) {
                    break;
                }

                if (blocks1[i] == blocks2[j]) {
                    return true;
                }
            }
        }

        return false;
    }

    public double getAggregateCardinality() {
        return aggregateCardinality;
    }

    public double getBlockAssignments() {
        return blockAssignments;
    }

    public double getD1BlockAssignments() {
        return d1BlockAssignments;
    }

    public double getD2BlockAssignments() {
        return d2BlockAssignments;
    }

    public double getPc() {
        return pc;
    }

    public double getPq() {
        return pq;
    }

    private void getBilateralBlockingCardinality() {
        d1BlockAssignments = 0;
        d2BlockAssignments = 0;
        for (AbstractBlock block : blocks) {
            BilateralBlock bilBlock = (BilateralBlock) block;
            d1BlockAssignments += bilBlock.getIndex1Entities().length;
            d2BlockAssignments += bilBlock.getIndex2Entities().length;
        }
    }

    private void getDecomposedBlocksEntities() {
        final TIntSet entitiesD1 = new TIntHashSet((int) aggregateCardinality);
        if (isCleanCleanER) {
            final TIntSet entitiesD2 = new TIntHashSet((int) aggregateCardinality);
            for (AbstractBlock block : blocks) {
                final ComparisonIterator iterator = block.getComparisonIterator();
                while (iterator.hasNext()) {
                    Comparison comparison = iterator.next();
                    entitiesD1.add(comparison.getEntityId1());
                    entitiesD2.add(comparison.getEntityId2());
                }
            }
            noOfD1Entities = entitiesD1.size();
            noOfD2Entities = entitiesD2.size();
        } else {
            for (AbstractBlock block : blocks) {
                final ComparisonIterator iterator = block.getComparisonIterator();
                while (iterator.hasNext()) {
                    Comparison comparison = iterator.next();
                    entitiesD1.add(comparison.getEntityId1());
                    entitiesD1.add(comparison.getEntityId2());
                }
            }
            noOfD1Entities = entitiesD1.size();
        }
    }

    public int getDetectedDuplicates() {
        return detectedDuplicates;
    }

    private void getDuplicatesOfDecomposedBlocks() {
        for (AbstractBlock block : blocks) {
            ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                abstractDP.isSuperfluous(iterator.next());
            }
        }

        detectedDuplicates = abstractDP.getNoOfDuplicates();
        pc = ((double) abstractDP.getNoOfDuplicates()) / abstractDP.getExistingDuplicates();
        pq = abstractDP.getNoOfDuplicates() / aggregateCardinality;
        if (0 < pc && 0 < pq) {
            fMeasure = 2 * pc * pq / (pc + pq);
        } else {
            fMeasure = 0;
        }
    }

    private void getDuplicatesWithEntityIndex() {
        double noOfDuplicates = 0;
        boolean cleanCleanER = blocks.get(0) instanceof BilateralBlock;
        for (IdDuplicates pairOfDuplicates : abstractDP.getDuplicates()) {
            if (areCooccurring(cleanCleanER, pairOfDuplicates)) {
                noOfDuplicates++;
            }
        }

        detectedDuplicates = (int) noOfDuplicates;
        pc = noOfDuplicates / abstractDP.getExistingDuplicates();
        pq = noOfDuplicates / aggregateCardinality;
        fMeasure = 2 * pc * pq / (pc + pq);
    }

    private void getEntities() {
        if (blocks.get(0) instanceof UnilateralBlock) {
            final TIntSet distinctEntities = new TIntHashSet();
            for (AbstractBlock block : blocks) {
                final UnilateralBlock uBlock = (UnilateralBlock) block;
                for (int entityId : uBlock.getEntities()) {
                    distinctEntities.add(entityId);
                }
            }
            noOfD1Entities = distinctEntities.size();
        } else {
            final TIntSet distinctEntitiesD1 = new TIntHashSet();
            final TIntSet distinctEntitiesD2 = new TIntHashSet();
            for (AbstractBlock block : blocks) {
                final BilateralBlock bBlock = (BilateralBlock) block;
                for (int entityId : bBlock.getIndex1Entities()) {
                    distinctEntitiesD1.add(entityId);
                }
                for (int entityId : bBlock.getIndex2Entities()) {
                    distinctEntitiesD2.add(entityId);
                }
            }
            noOfD1Entities = distinctEntitiesD1.size();
            noOfD2Entities = distinctEntitiesD2.size();
        }
    }

    public void printDetailedResults(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2) {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType();

        List<AbstractBlock> blocksToUse = blocks;
        if (!(blocks.get(0) instanceof DecomposedBlock)) {
            final ComparisonPropagation cp = new ComparisonPropagation();
            blocksToUse = cp.refineBlocks(blocks);
        }

        abstractDP.resetDuplicates();
        for (AbstractBlock block : blocksToUse) {
            ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                final Comparison currentComparison = iterator.next();
                final EntityProfile profile1 = profilesD1.get(currentComparison.getEntityId1());
                final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(currentComparison.getEntityId2()) : profilesD1.get(currentComparison.getEntityId2());

                final int originalDuplicates = abstractDP.getNoOfDuplicates();
                abstractDP.isSuperfluous(currentComparison);
                final int newDuplicates = abstractDP.getNoOfDuplicates();

                System.out.print(profile1.getEntityUrl() + ",");
                System.out.print(profile2.getEntityUrl() + ",");
                if (originalDuplicates == newDuplicates) {
                    System.out.print("FP,"); //false positive
                } else { // originalDuplicates < newDuplicates
                    System.out.print("TP,"); // true positive
                }
                System.out.print("Profile 1:[" + profilesD1 + "]");
                System.out.println("Profile 2:[" + profilesD2 + "]");
            }
        }

        for (IdDuplicates duplicatesPair : abstractDP.getFalseNegatives()) {
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            System.out.print(profile1.getEntityUrl() + ",");
            System.out.print(profile2.getEntityUrl() + ",");
            System.out.print("FN,"); // false negative
            System.out.print("Profile 1:[" + profile1 + "]");
            System.out.println("Profile 2:[" + profile2 + "]");
        }

        detectedDuplicates = abstractDP.getNoOfDuplicates();
        pc = ((double) abstractDP.getNoOfDuplicates()) / abstractDP.getExistingDuplicates();
        pq = abstractDP.getNoOfDuplicates() / aggregateCardinality;
        if (0 < pc && 0 < pq) {
            fMeasure = 2 * pc * pq / (pc + pq);
        } else {
            fMeasure = 0;
        }

        System.out.println("Pairs Quality (Precision)\t:\t" + pq);
        System.out.println("Pairs Completentess (Recall)\t:\t" + pc);
        System.out.println("F-Measure\t:\t" + fMeasure);
    }

    public void printStatistics(double overheadTime, String methodConfiguration, String methodName) {
        if (blocks.isEmpty()) {
            return;
        }

        System.out.println("\n\n\n**************************************************");
        System.out.println("Performance of : " + methodName);
        System.out.println("Configuration : " + methodConfiguration);
        System.out.println("**************************************************");
        System.out.println("No of blocks\t:\t" + blocks.size());
        System.out.println("Aggregate cardinality\t:\t" + aggregateCardinality);
        System.out.println("CC\t:\t" + (blockAssignments / aggregateCardinality));
        if (blocks.get(0) instanceof BilateralBlock) {
            System.out.println("Total entities D1\t:\t" + entityIndex.getDatasetLimit());
            System.out.println("Singleton entities D1\t:\t" + (entityIndex.getDatasetLimit() - noOfD1Entities));
            System.out.println("Total entities D2\t:\t" + (entityIndex.getNoOfEntities() - entityIndex.getDatasetLimit()));
            System.out.println("Singleton entities D2\t:\t" + (entityIndex.getNoOfEntities() - entityIndex.getDatasetLimit() - noOfD2Entities));
            System.out.println("Entities in blocks\t:\t" + (noOfD1Entities + noOfD2Entities));
            System.out.println("Average block\t:\t" + d1BlockAssignments / blocks.size() + "-" + d2BlockAssignments / blocks.size());
            System.out.println("iBC_1\t:\t" + d1BlockAssignments / noOfD1Entities);
            System.out.println("iBC_2\t:\t" + d2BlockAssignments / noOfD2Entities);
            System.out.println("oBC\t:\t" + ((d1BlockAssignments + d2BlockAssignments) / (noOfD1Entities + noOfD2Entities)));
        } else if (blocks.get(0) instanceof DecomposedBlock) {
            if (isCleanCleanER) {
                System.out.println("Entities in blocks\t:\t" + (noOfD1Entities + noOfD2Entities));
            } else {
                System.out.println("Entities in blocks\t:\t" + noOfD1Entities);
            }
        } else if (blocks.get(0) instanceof UnilateralBlock) {
            System.out.println("Total entities\t:\t" + entityIndex.getNoOfEntities());
            System.out.println("Entities in blocks\t:\t" + noOfD1Entities);
            System.out.println("Singleton entities\t:\t" + (entityIndex.getNoOfEntities() - noOfD1Entities));
            System.out.println("Average block\t:\t" + blockAssignments / blocks.size());
            System.out.println("BC\t:\t" + blockAssignments / noOfD1Entities);
        }
        System.out.println("Detected duplicates\t:\t" + detectedDuplicates);
        System.out.println("PC\t:\t" + pc);
        System.out.println("PQ\t:\t" + pq);
        System.out.println("F-Measure\t:\t" + fMeasure);
        System.out.println("Overhead time\t:\t" + overheadTime);
    }

    private void setComparisonsCardinality() {
        aggregateCardinality = 0;
        blockAssignments = 0;
        for (AbstractBlock block : blocks) {
            aggregateCardinality += block.getNoOfComparisons();
            blockAssignments += block.getTotalBlockAssignments();
        }
    }

    public void setStatistics() {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType();
        setComparisonsCardinality();
        if (blocks.get(0) instanceof DecomposedBlock) {
            getDecomposedBlocksEntities();
        } else {
            entityIndex = new GroundTruthIndex(blocks, abstractDP.getDuplicates());
            getEntities();
        }
        if (blocks.get(0) instanceof BilateralBlock) {
            getBilateralBlockingCardinality();
        }
        if (blocks.get(0) instanceof DecomposedBlock) {
            getDuplicatesOfDecomposedBlocks();
        } else {
            getDuplicatesWithEntityIndex();
        }
    }

    private void setType() {
        if (blocks.get(0) instanceof BilateralBlock) {
            isCleanCleanER = true;
        } else if (blocks.get(0) instanceof DecomposedBlock) {
            DecomposedBlock deBlock = (DecomposedBlock) blocks.get(0);
            isCleanCleanER = deBlock.isCleanCleanER();
        } else if (blocks.get(0) instanceof UnilateralBlock) {
            isCleanCleanER = false;
        }
    }
}
