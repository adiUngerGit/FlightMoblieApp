/*using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using FlightMobileWeb.Server;
using FlightMobileWeb.ClientSide;
using Microsoft.AspNetCore.Mvc;
using System.IO;
using System.Net;
using System.Text;
using Microsoft.Extensions.Options;

namespace FlightMobileWeb.Controllers
{
    [Route("[controller]")]
    [ApiController]
    public class ScreenshotController : ControllerBase
    {
        private ClientScreenshot client;
        public ScreenshotController(ClientScreenshot _client)
        {
            client = _client;
        }

        // POST screenshot
        [HttpGet]
        public async Task<IActionResult> Get()
        {
            byte[] task = await client.getScreenshot();
            if (task == null)
            {
                return BadRequest();
            }
            try
            {
                 return Ok(File(task, "image/jpeg"));
               // return Ok(5);
            }
            catch
            {
                return BadRequest();
            }

        }
    }
}
*/


using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using FlightMobileWeb.Server;
using FlightMobileWeb.ClientSide;
using Microsoft.AspNetCore.Mvc;
using System.IO;
using System.Net;
using System.Text;
using Microsoft.Extensions.Options;
using System.Net.Http;

namespace FlightMobileWeb.Controllers
{
    [Route("[controller]")]
    [ApiController]
    public class ScreenshotController : ControllerBase
    {
        //private ClientScreenshot client;
        private HttpClient clientHttp;
        private readonly Config _myConfiguration;

        public ScreenshotController(IOptions<Config> myConfiguration)
        {
            //client = _client;
            clientHttp = new HttpClient();
            _myConfiguration = myConfiguration.Value;

        }

        // POST screenshot
        [HttpGet]
        public  IActionResult Get()
        {

            try
            {
                  string[] conf = _myConfiguration.Urlhttp.Split(":");

                  System.IO.Stream respone = clientHttp.GetStreamAsync("http://" + conf[0] + ":" + conf[1] + "/screenshot").Result;

                 return Ok(respone);
                //return Ok(1);
            }
            catch (HttpRequestException){
                return BadRequest();
            }
        

        }
    }
}