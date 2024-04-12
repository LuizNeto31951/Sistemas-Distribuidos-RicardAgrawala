import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;


public class RicartAgrawala {
    public static void main(String[] args) {
        int numProcesses = 5;

        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        Map<Integer, Process> processes = new HashMap<>();
        Random random = new Random();

        // Atribui números aleatórios únicos a cada processo para definir a prioridade
        int[] priorities = new int[numProcesses];
        for (int i = 0; i < numProcesses; i++) {
            priorities[i] = i; // Números de 0 a 4
        }
        for (int i = numProcesses - 1; i > 0; i--) {
            // Embaralha os números para garantir aleatoriedade
            int j = random.nextInt(i + 1);
            int temp = priorities[i];
            priorities[i] = priorities[j];
            priorities[j] = temp;
        }

        // Cria os processos
        for (int i = 0; i < numProcesses; i++) {
            int priority = priorities[i];
            Process process = new Process(i, priority, lock, processes, condition);
            processes.put(i, process);
            Thread thread = new Thread(process);
            thread.start();
        }
    }
}
