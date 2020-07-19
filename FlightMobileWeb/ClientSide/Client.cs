using FlightMobileWeb.Server;
using Microsoft.AspNetCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Options;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net.Sockets;
using System.Text;
using System.Threading.Tasks;

namespace FlightMobileWeb.ClientSide
{
    public class Client : IClient
    {
        private TcpClient tcpClient;
        private BlockingCollection<AsyncCommand> _queue;
        private NetworkStream stream;
        private Result res = Result.NotOk;
        private string urls;
        private readonly Config _myConfiguration;
        private bool error = false;

        public Client(IOptions<Config> myConfiguration)
        {
            _myConfiguration = myConfiguration.Value;

            urls = _myConfiguration.Urls;
            _queue = new BlockingCollection<AsyncCommand>();
            tcpClient = new TcpClient();
            Start();

        }
        public Result getError()
        {
            return res;
        }
        private readonly List<string> steeringWheel = new List<string>()
        {
            "/controls/engines/current-engine/throttle",
            "/controls/flight/elevator",
            "/controls/flight/rudder",
            "/controls/flight/aileron"
        };
        public Task<Result> Execute(Command cmd)
        {
            var asyncCommand = new AsyncCommand(cmd);
            _queue.Add(asyncCommand);
            return asyncCommand.Task;
        }
        public void Start()
        {

            Task.Factory.StartNew(ProcessCommands);

        }
        public void ProcessCommands()
        {

            try
            {
                Connet();
                res = Result.Ok;
            }
            catch (SocketException)
            {
                res = Result.NotOk;
                return;

            }
            stream = tcpClient.GetStream();
            byte[] sendBuffer = new byte[1024];
            sendBuffer = Encoding.ASCII.GetBytes("data\n");
            stream.Write(sendBuffer, 0, sendBuffer.Length);
            foreach (AsyncCommand command in _queue.GetConsumingEnumerable())
            {
                List<string> commandList = FromCommandToString(command.Command);
                Result temp = Result.Ok;

                for (int i = 0; i < 4; i++)
                {
                    try
                    {
                        temp = readAndWrite(commandList[i], steeringWheel[i]);
                    }
                    catch (TimeoutException)
                    {
                        res = Result.NotOk;
                        return;
                    }
                    if (temp == Result.NotOk)
                    {
                        error = true;
                    }

                }
                if (error)
                {
                    res = Result.NotOk;
                }
                command.Completion.SetResult(res);
            }

        }

        public Result readAndWrite(string com, string command)
        {
            //com = commandList[i];
            byte[] sendBuffer = new byte[1024];
            sendBuffer = new byte[1024];
            sendBuffer = Encoding.ASCII.GetBytes(com);
            byte[] recvBuffer = new byte[1024];
            Stopwatch watch = new Stopwatch();
            tcpClient.ReceiveTimeout = 10000;
            watch.Start();
            try
            {
                stream.Write(sendBuffer, 0, sendBuffer.Length);
                ReadFromServer(command);

            }
            catch (IOException)
            {
                throw new TimeoutException();
            }
            DateTime end = DateTime.UtcNow;
            watch.Stop();

            if (recvBuffer == null)
            {
                res = Result.NotOk;
            }
            else
            {
                res = Result.Ok;
            }
            return res;
        }
        public List<string> FromCommandToString(Command command)
        {
            List<string> commandList = new List<string>();
            string s = "set " + steeringWheel[0] + " " + command.Throttle + "\n";
            commandList.Add(s);
            s = "set " + steeringWheel[1] + " " + command.Elevator + "\n";
            commandList.Add(s);
            s = "set " + steeringWheel[2] + " " + command.Rudder + "\n";
            commandList.Add(s);
            s = "set " + steeringWheel[3] + " " + command.Aileron + "\n";
            commandList.Add(s);

            return commandList;
        }
        public void Connet()
        {
            string[] con = urls.Split(":");
            tcpClient.Connect(con[0], Convert.ToInt32(con[1]));

        }

        public void Dissconect()
        {
            tcpClient.Close();
        }

        public string ReadFromServer(string str)
        {
            string command = "get " + str + "\n";
            byte[] read = new byte[1024];
            read = Encoding.ASCII.GetBytes(command);
            tcpClient.GetStream().Write(read, 0, read.Length);
            string responseData = String.Empty;
            byte[] data = new Byte[1024];
            Int32 bytes = tcpClient.GetStream().Read(data, 0, data.Length);
            responseData = System.Text.Encoding.ASCII.GetString(data, 0, bytes);
            return responseData;
        }

        public void WriteFromServer(string str)
        {
            NetworkStream serverStream = tcpClient.GetStream();
            Byte[] command = System.Text.Encoding.ASCII.GetBytes(str);
            serverStream.Write(command, 0, command.Length);
            command = new Byte[1024];
        }
    }
}
