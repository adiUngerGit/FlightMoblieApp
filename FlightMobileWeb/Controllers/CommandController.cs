using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using FlightMobileWeb.Server;
using FlightMobileWeb.ClientSide;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Options;
using System.IO;

namespace FlightMobileWeb.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class CommandController : ControllerBase
    {
        private Client client;

        public CommandController(Client _client)
        {
            try
            {
                client = _client;
            }
            catch
            {
                throw new Exception("error in connecting");
            }


        }
        // POST api/command
        [HttpPost]
        public IActionResult Post([FromBody] Command command)
        {
            try
            {
                Task<Result> taskResult = client.Execute(command);
                Console.WriteLine(command);

                if (client.getError() == Result.NotOk)
                {
                    return BadRequest();
                }
                return Ok();
            }
            catch (TimeoutException)
            {
                return BadRequest();
            }
            catch
            {
                return BadRequest();
            }
        }
    }
}
