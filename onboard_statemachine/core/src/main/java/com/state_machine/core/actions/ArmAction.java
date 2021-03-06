package com.state_machine.core.actions;

import com.state_machine.core.actions.util.ActionStatus;
import com.state_machine.core.droneState.DroneStateTracker;
import mavros_msgs.CommandBoolRequest;
import mavros_msgs.CommandBoolResponse;
import org.ros.exception.RemoteException;
import org.ros.message.Time;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import org.apache.commons.logging.Log;

public class ArmAction extends Action {

    private DroneStateTracker stateTracker;
    private ServiceClient<CommandBoolRequest, CommandBoolResponse> armingService;
    private Log logger;
    private int retryCount;

    public ArmAction(
            Log logger,
            ServiceClient<CommandBoolRequest, CommandBoolResponse> armingService,
            DroneStateTracker stateTracker
    ){
        super();
        retryCount = 0;
        this.logger = logger;
        this.stateTracker = stateTracker;
        this.armingService = armingService;
    }

    public ActionStatus loopAction(Time time){
        if (stateTracker.getArmed()) {
            return ActionStatus.Success;
        }
        else {
            if (!armingService.isConnected()) return ActionStatus.ConnectionFailure;


            if(retryCount == 10)    //if try to arm for 10 times, return failure
                return ActionStatus.Failure;

            CommandBoolRequest message = armingService.newMessage();
            message.setValue(true);
            
            ServiceResponseListener<CommandBoolResponse> listener = new ServiceResponseListener<CommandBoolResponse>() {
                @Override
                public void onSuccess(CommandBoolResponse commandBoolResponse) {
                    serviceResult = ActionStatus.Success;
                }

                @Override
                public void onFailure(RemoteException e) {
                    serviceResult = ActionStatus.Failure;
                }
            };
            armingService.call(message, listener);

            stateTracker.setLocalOrigin(stateTracker.getVisionPosition());
            retryCount += 1;
            if(serviceResult == ActionStatus.Success){
                return ActionStatus.Success;
            }else{
                return ActionStatus.Running;
            }
        }
    }

    public String toString() {
        return "ArmAction";
    }
}
