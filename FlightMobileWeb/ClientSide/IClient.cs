using FlightMobileWeb.Server;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace FlightMobileWeb.ClientSide
{
    interface IClient
    {
        void Connet();
        void WriteFromServer(string str);
        string ReadFromServer(string str);
        void Dissconect();
        Task<Result> Execute(Command cmd);
    }
}
