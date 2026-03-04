package org.solop.queue;

import org.compiere.util.CLogger;
import org.solop.forecast.engine.ForecastEngineManager;
import org.solop.forecast.engine.IForecastEngine;
import org.spin.queue.model.MADQueue;
import org.spin.queue.util.QueueManager;


/**
 * Forecast Comparison Processor (FCC)
 * @author Gabriel Escalona
 */
public class ForecastComparisonProcessor extends QueueManager {
    public static String QueueType_ForecastComparison = "FCC";


    /**	Logger						*/
    protected CLogger log = CLogger.getCLogger(getClass());
    @Override
    public void add(int queueId) {
        log.fine("Forecast Queue Added: " + queueId);
    }

    @Override
    public void process(int queueId) {
        MADQueue queue = new MADQueue(getContext(), queueId, getTransactionName());
        //  Ignore without record
        if(queue.getAD_Table_ID() <= 0 || queue.getRecord_ID() <= 0) {
            return;
        }
        IForecastEngine forecastEngine = ForecastEngineManager.getForecastEngine(getContext(), queue.getAD_Client_ID());
        if (forecastEngine != null) {
            forecastEngine.forecastRun(getContext(), queue.getAD_Table_ID(), queue.getRecord_ID(), getTransactionName());
        }
    }
}
