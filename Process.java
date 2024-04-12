import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;

class Process implements Runnable {
    private final int id;
    private final int priority; // Prioridade do processo (número aleatório único entre 0 e 4)
    private final Lock lock;
    private final Map<Integer, Boolean> requestPermissions; // Vetor de permissões recebidas
    private final Map<Integer, Process> processes; // Mapa de processos
    private final Condition condition;

    public Process(int id, int priority, Lock lock, Map<Integer, Process> processes, Condition condition) {
        this.id = id;
        this.priority = priority;
        this.lock = lock;
        this.processes = processes;
        this.requestPermissions = new ConcurrentHashMap<>();
        this.condition = condition;

        // Inicializa as permissões como falsas
        for (int pid : processes.keySet()) {
            if (pid != id) {
                requestPermissions.put(pid, false);
            }
        }
    }

    @Override
    public void run() {
        try {

            lock.lock();
            try {
                // Solicita permissão aos outros processos
                requestPermissionsFromOthers();
                System.out.println("Processo " + id + " solicitou permissão aos outros processos.");

                // Aguarda até receber permissão de todos os outros processos
                while (!hasAllPermissions() || requestPermissions.size() <= 3) {
                    Thread.sleep(1000);
                    requestPermissionsFromOthers();
                    Thread.sleep(500);
                    System.out.println("Processo " + id + " aguardando permissão. Fila de requisições pendentes: "
                            + requestPermissions + " Prioridade " + priority);
                    condition.await(); // Aguarda até receber todas as permissões
                }

                // Processo entra na seção crítica
                long timestamp = System.currentTimeMillis();
                String formattedTimestamp = formatDate(timestamp);
                System.out.println("Processo " + id + " entrou na seção crítica no tempo " + formattedTimestamp
                        + " Motivo: " + requestPermissions + "Prioridade " + priority);

                // Simula processamento na seção crítica
                Thread.sleep(2000);

            } finally {
                // Processo sai da seção crítica
                sendPermissionsToOthers();
                lock.unlock();
                long timestamp = System.currentTimeMillis();
                String formattedTimestamp = formatDate(timestamp);
                System.out.println("Processo " + id + " saiu da seção crítica no tempo " + formattedTimestamp);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void requestPermissionsFromOthers() {
        for (int pid : processes.keySet()) {
            if (pid != id) {
                // Solicita permissão de cada processo
                processes.get(pid).receiveRequest(id, priority);
            }
        }
    }

    private boolean hasAllPermissions() {
        for (boolean permission : requestPermissions.values()) {
            if (!permission) {
                return false;
            }
        }
        return true;
    }

    private void sendPermissionsToOthers() {
        for (int pid : processes.keySet()) {
            if (pid != id) {
                processes.get(pid).receivePermission(id);
            }
        }
    }

    // Recebe solicitação de permissão de outro processo
    public synchronized void receiveRequest(int requestingId, int requestingPriority) {
        lock.lock();
        try {
            // Verifica se a prioridade do solicitante é maior ou igual à do processo atual
            if (requestingPriority > priority || (requestingPriority == priority && requestingId > id)) {
                // Concede permissão ao processo solicitante
                requestPermissions.put(requestingId, true);
                condition.signalAll(); // Notifica threads aguardando
            } else {
                // Registra a solicitação pendente
                requestPermissions.put(requestingId, false);
            }
        } finally {
            lock.unlock();
        }
    }

    // Recebe permissão de outro processo
    public synchronized void receivePermission(int sendingId) {
        lock.lock();
        try {
            requestPermissions.put(sendingId, true);
            condition.signalAll(); // Notifica threads aguardando
        } finally {
            lock.unlock();
        }
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        return sdf.format(new Date(timestamp));
    }
}