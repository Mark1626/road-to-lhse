/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mark;

import io.airlift.configuration.Config;

public class RecutilConfig
{
    private String protocol;
    private String base;
    private String username;
    private String password;
    private String host;
    private Integer port;
    private String suffix;

    public String getProtocol()
    {
        return protocol;
    }

    public String getBase()
    {
        return base;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public String getHost()
    {
        return host;
    }

    public Integer getPort()
    {
        return port;
    }

    public String getSuffix()
    {
        return suffix;
    }

    @Config("rec.protocol")
    public RecutilConfig setProtocol(String protocol)
    {
        this.protocol = protocol;
        return this;
    }

    @Config("rec.base")
    public RecutilConfig setBase(String base)
    {
        this.base = base;
        return this;
    }

    @Config("rec.username")
    public RecutilConfig setUsername(String username)
    {
        this.username = username;
        return this;
    }

    @Config("rec.password")
    public RecutilConfig setPassword(String password)
    {
        this.password = password;
        return this;
    }

    @Config("rec.host")
    public RecutilConfig setHost(String host)
    {
        this.host = host;
        return this;
    }

    @Config("rec.port")
    public RecutilConfig setPort(Integer port)
    {
        this.port = port;
        return this;
    }

    @Config("rec.suffix")
    public RecutilConfig setSuffix(String suffix)
    {
        this.suffix = suffix;
        return this;
    }
}
