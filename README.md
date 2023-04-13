# Building Elevator Control System
This is a simulation of a building elevator control system. The system consists of a scheduler, elevator subsystem, floor subsystem, and an elevator view GUI. The scheduler receives requests from the floor subsystem and assigns them to elevators. The elevator subsystem receives requests from the scheduler and moves the elevators accordingly. The floor subsystem sends requests and issues to the scheduler and receives elevator arrival notifications. Additionally, updated information are forwarded to the elevator view to update the GUI.

## Getting Started
To run the simulation, you will need to have Java 8 or later installed on your system. Clone this repository to your local machine and navigate to the project directory.

* `$ git clone https://github.com/your-username/building-elevator-control-system.git`
* `$ cd building-elevator-control-system`

## Running the Simulation
To run the simulation, execute the following command in the project directory, which will start the simulation and display the simulation log in the console:
* `$ java -jar BuildingElevatorControlSystem.jar`

## Modifying the Simulation
You can modify the simulation by changing the constants defined in the Building class. For example, you can change the number of floors or elevators in the building by modifying the TOTAL_FLOOR and TOTAL_ELEVATOR constants, respectively.

The input text for a floor request must be in the following format:

| Time         | Floor | Direction | Elevator Number | Type              |
|--------------|-------|-----------|-----------------|-------------------|
| HH:MM:SS.MMM | n     | UP/DOWN   | n               | EXTERNAL/INTERNAL |   

The input for a door issue/elevator stuck must be in the following format:

| Time         | Floor | Door Issue                |
|--------------|-------|---------------------------|
| HH:MM:SS.MMM | n     | DOOR_ISSUE/ELEVATOR_STUCK |

## Contributing
Contributions are welcome! If you find a bug or would like to suggest an enhancement, please submit an issue or a pull request.

## License
This project is licensed under the MIT License. See the LICENSE file for more information.
